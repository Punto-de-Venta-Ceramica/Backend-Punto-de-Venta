package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.CorteCajaInvalidoException;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoPersistenceAdapter implements PedidoRepositorioPort, ConsultaPedidosPort {
    private final JdbcTemplate jdbc;
    private final SucursalContrato sucursales;
    public PedidoPersistenceAdapter(JdbcTemplate jdbc, SucursalContrato sucursales) {
        this.jdbc = jdbc;
        this.sucursales = sucursales;
    }

    @Override
    public void comprobarAcceso(long sucursalId, long usuarioId) {
        if (sucursales.negocioAutorizado(sucursalId, usuarioId).isEmpty()) {
            throw new AccessDeniedException("La sucursal no está disponible.");
        }
    }

    @Override
    public long guardar(DatosRegistro datos, long clienteId, com.iaml.puntoventaapi.pedidos.domain.Pedido pedido) {
        Long id = jdbc.queryForObject("""
            insert into barrocontrol.pedido (id_sucursal, id_usuario, id_cliente, folio, descripcion,
                 tipo_entrega, estado, descripcion_envio, fecha_creacion)
            values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp) returning id
            """, Long.class, datos.sucursalId(), datos.usuarioId(), clienteId, UUID.randomUUID().toString(),
            datos.descripcion(), datos.tipoEntrega(), pedido.estado().name(), datos.descripcionEnvio());
        if (id == null) throw new IllegalStateException("No se pudo guardar el pedido.");
        for (var detalle : pedido.detalles()) {
            jdbc.update("""
                insert into barrocontrol.detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, desc_aplicado)
                values (?, ?, ?, ?, ?)
                """, id, detalle.productoId(), detalle.cantidad(), detalle.precioUnitario(), detalle.porcentajeDescuento());
        }
        for (var pago : pedido.pagos()) insertarPago(id, pago, pedido.pagado(), datos.corteCajaId());
        return id;
    }

    @Override
    public com.iaml.puntoventaapi.pedidos.domain.Pedido obtenerParaActualizar(long pedidoId, long usuarioId) {
        long sucursalId = sucursalDelPedido(pedidoId);
        comprobarAccesoPedido(sucursalId, usuarioId);
        String estado = jdbc.queryForObject("select estado from barrocontrol.pedido where id = ? for update", String.class, pedidoId);
        var detalles = jdbc.query("""
            select id_producto, cantidad, precio_unitario, desc_aplicado
            from barrocontrol.detalle_pedido where id_pedido = ? order by id_producto
            """, (rs, i) -> new com.iaml.puntoventaapi.pedidos.domain.DetallePedido(rs.getLong(1), rs.getInt(2),
                rs.getBigDecimal(3), rs.getBigDecimal(4)), pedidoId);
        var pagos = jdbc.query("select monto, metodo_pago from barrocontrol.pago_pedido where id_pedido = ? order by id",
            (rs, i) -> new PagoPedido(rs.getBigDecimal(1), PagoPedido.MetodoPago.valueOf(rs.getString(2))), pedidoId);
        return com.iaml.puntoventaapi.pedidos.domain.Pedido.restaurar(detalles, pagos, EstadoPedido.valueOf(estado));
    }

    @Override
    public boolean esPropietarioActual(long pedidoId, long usuarioId) {
        return !jdbc.queryForList("""
            select u.id from barrocontrol.usuario u
            join barrocontrol.rol r on r.id = u.id_rol
            join barrocontrol.negocio_usuario nu on nu.id_usuario = u.id
            join barrocontrol.sucursal s on s.id_negocio = nu.id_negocio
            join barrocontrol.pedido p on p.id_sucursal = s.id
            where u.id = ? and p.id = ? and r.nombre = 'PROPIETARIO'
            for share of u
            """, Long.class, usuarioId, pedidoId).isEmpty();
    }

    @Override
    public void guardarPago(long pedidoId, PagoPedido pago, long corteCajaId) {
        BigDecimal saldo = jdbc.queryForObject("select saldo from barrocontrol.vw_pedido where id = ?", BigDecimal.class, pedidoId);
        if (saldo == null) throw new IllegalStateException("No se pudo calcular el saldo.");
        insertarPago(pedidoId, pago, pago.monto().compareTo(saldo) == 0, corteCajaId);
    }

    private void insertarPago(long pedidoId, PagoPedido pago, boolean liquidado, Long corteCajaId) {
        if (corteCajaId == null) throw new CorteCajaInvalidoException();
        var cortes = jdbc.queryForList("""
            select cc.id from barrocontrol.corte_caja cc
            join barrocontrol.caja c on c.id = cc.id_caja
            join barrocontrol.pedido p on p.id_sucursal = c.id_sucursal
            where p.id = ? and cc.id = ? and cc.fecha_hora_cierre is null
            for update of cc
            """, Long.class, pedidoId, corteCajaId);
        if (cortes.isEmpty()) throw new CorteCajaInvalidoException();
        jdbc.update("""
            insert into barrocontrol.pago_pedido (id_pedido, id_corte_caja, monto, metodo_pago, fecha_hora, tipo_pago)
            values (?, ?, ?, ?, current_timestamp, ?)
            """, pedidoId, corteCajaId, pago.monto(), pago.metodoPago().name(), liquidado ? "LIQUIDACION" : "ANTICIPO");
    }

    @Override
    public void guardarEstado(long pedidoId, EstadoPedido estado) {
        jdbc.update("""
            update barrocontrol.pedido set estado = ?,
                fecha_entrega = case when ? = 'ENTREGADO' then coalesce(fecha_entrega, current_timestamp) else fecha_entrega end
            where id = ?
            """, estado.name(), estado.name(), pedidoId);
    }

    private long sucursalDelPedido(long pedidoId) {
        try {
            Long id = jdbc.queryForObject("select id_sucursal from barrocontrol.pedido where id = ?", Long.class, pedidoId);
            if (id == null) throw new PedidoNoEncontradoException();
            return id;
        } catch (EmptyResultDataAccessException ex) {
            throw new PedidoNoEncontradoException();
        }
    }

    @Override
    public Vista consultar(long pedidoId, long usuarioId) {
        comprobarAccesoPedido(sucursalDelPedido(pedidoId), usuarioId);
        var cabeceras = jdbc.query("""
            select p.id, p.id_sucursal, p.folio, p.descripcion, p.tipo_entrega,
                   p.descripcion_envio, p.estado, p.fecha_creacion,
                   c.nombre as cliente_nombre, c.telefono, c.ciudad, c.colonia, c.calle,
                   v.total, v.total_pagado, v.saldo
            from barrocontrol.pedido p
            join barrocontrol.cliente c on c.id = p.id_cliente
            join barrocontrol.vw_pedido v on v.id = p.id where p.id = ?
            """, (rs, i) -> cabecera(rs), pedidoId);
        if (cabeceras.isEmpty()) throw new PedidoNoEncontradoException();
        return cabeceras.get(0);
    }

    private Vista cabecera(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        var cliente = new Cliente(rs.getString("cliente_nombre"), rs.getString("telefono"), rs.getString("ciudad"),
            rs.getString("colonia"), rs.getString("calle"));
        var productos = jdbc.query("""
            select d.id_producto, p.nombre, d.cantidad, d.precio_unitario, d.desc_aplicado, d.subtotal
            from barrocontrol.detalle_pedido d join barrocontrol.producto p on p.id = d.id_producto
            where d.id_pedido = ? order by d.id_producto
            """, (r, i) -> new Detalle(r.getLong(1), r.getString(2), r.getInt(3), r.getBigDecimal(4),
                r.getBigDecimal(5), r.getBigDecimal(6)), id);
        var pagos = jdbc.query("""
            select id, monto, metodo_pago, tipo_pago, fecha_hora from barrocontrol.pago_pedido
            where id_pedido = ? order by id
            """, (r, i) -> new Pago(r.getLong(1), r.getBigDecimal(2), r.getString(3), r.getString(4),
                r.getTimestamp(5).toInstant()), id);
        BigDecimal saldo = rs.getBigDecimal("saldo");
        return new Vista(id, rs.getLong("id_sucursal"), rs.getString("folio"),
            rs.getString("descripcion"), rs.getString("tipo_entrega"), rs.getString("descripcion_envio"),
            rs.getString("estado"), rs.getTimestamp("fecha_creacion").toInstant(), cliente, productos, pagos,
            rs.getBigDecimal("total"), rs.getBigDecimal("total_pagado"), saldo, saldo.signum() == 0);
    }

    @Override
    public List<Vista> listar(long sucursalId, long usuarioId) {
        comprobarAcceso(sucursalId, usuarioId);
        var ids = jdbc.queryForList("""
            select p.id from barrocontrol.pedido p
            where p.id_sucursal = ? order by p.fecha_creacion desc, p.id desc limit 100
            """, Long.class, sucursalId);
        return ids.stream().map(id -> consultar(id, usuarioId)).toList();
    }

    private void comprobarAccesoPedido(long sucursalId, long usuarioId) {
        if (sucursales.negocioAutorizado(sucursalId, usuarioId).isEmpty()) throw new PedidoNoEncontradoException();
    }
}
