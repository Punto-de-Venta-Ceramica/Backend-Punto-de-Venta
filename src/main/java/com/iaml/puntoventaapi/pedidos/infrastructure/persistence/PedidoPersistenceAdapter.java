package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.CorteCajaInvalidoException;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoPersistenceAdapter implements PedidoRepositorioPort, ConsultaPedidosPort {
    /**
     * Tipo de pago con el que se registra lo devuelto al cancelar un pedido. Va en el
     * corte abierto al cancelar, para descontarlo ahÃ­ aunque el cobro sea de otro corte.
     * No es un cobro: se excluye de lo pagado.
     */
    private static final String DEVOLUCION = "DEVOLUCION";

    /**
     * Total y lo pagado del pedido {@code p}, sin contar devoluciones. Sustituye a la
     * vista vw_pedido, que suma todas las filas de pago_pedido.
     */
    private static final String TOTALES = """
        cross join lateral (select coalesce(sum(d.subtotal), 0) as total
                            from barrocontrol.detalle_pedido d where d.id_pedido = p.id) tot
        cross join lateral (select coalesce(sum(pg.monto), 0) as total_pagado
                            from barrocontrol.pago_pedido pg
                            where pg.id_pedido = p.id and pg.tipo_pago <> 'DEVOLUCION') pag
        """;

    private final JdbcTemplate jdbc;
    private final SucursalContrato sucursales;
    public PedidoPersistenceAdapter(JdbcTemplate jdbc, SucursalContrato sucursales) {
        this.jdbc = jdbc;
        this.sucursales = sucursales;
    }

    @Override
    public void comprobarAcceso(long sucursalId, long usuarioId) {
        if (sucursales.negocioAutorizado(sucursalId, usuarioId).isEmpty()) {
            throw new AccessDeniedException("La sucursal no estÃ¡ disponible.");
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
        for (var pago : pedido.pagos()) insertarPago(id, pago, pedido.pagado());
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
        var pagos = jdbc.query("""
            select monto, metodo_pago from barrocontrol.pago_pedido
            where id_pedido = ? and tipo_pago <> 'DEVOLUCION' order by id
            """, (rs, i) -> new PagoPedido(rs.getBigDecimal(1), PagoPedido.MetodoPago.valueOf(rs.getString(2))), pedidoId);
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
    public void guardarPago(long pedidoId, PagoPedido pago) {
        BigDecimal saldo = jdbc.queryForObject(
            "select tot.total - pag.total_pagado from barrocontrol.pedido p\n" + TOTALES + "where p.id = ?",
            BigDecimal.class, pedidoId);
        if (saldo == null) throw new IllegalStateException("No se pudo calcular el saldo.");
        insertarPago(pedidoId, pago, pago.monto().compareTo(saldo) == 0);
    }

    private void insertarPago(long pedidoId, PagoPedido pago, boolean liquidado) {
        Long corteCajaId = corteAbierto(pedidoId).orElseThrow(CorteCajaInvalidoException::new);
        jdbc.update("""
            insert into barrocontrol.pago_pedido (id_pedido, id_corte_caja, monto, metodo_pago, fecha_hora, tipo_pago)
            values (?, ?, ?, ?, current_timestamp, ?)
            """, pedidoId, corteCajaId, pago.monto(), pago.metodoPago().name(), liquidado ? "LIQUIDACION" : "ANTICIPO");
    }

    /** Corte abierto de la sucursal del pedido, bloqueado hasta terminar la transacciÃ³n. */
    private Optional<Long> corteAbierto(long pedidoId) {
        return jdbc.queryForList("""
            select cc.id from barrocontrol.corte_caja cc
            join barrocontrol.caja c on c.id = cc.id_caja
            join barrocontrol.pedido p on p.id_sucursal = c.id_sucursal
            where p.id = ? and cc.fecha_hora_cierre is null
            order by cc.fecha_hora_apertura desc, cc.id desc
            limit 1
            for update of cc
            """, Long.class, pedidoId).stream().findFirst();
    }

    @Override
    public void guardarEstado(long pedidoId, EstadoPedido estado) {
        if (estado == EstadoPedido.CANCELADO) registrarDevolucion(pedidoId);
        jdbc.update("""
            update barrocontrol.pedido set estado = ?,
                fecha_entrega = case when ? = 'ENTREGADO' then coalesce(fecha_entrega, current_timestamp) else fecha_entrega end
            where id = ?
            """, estado.name(), estado.name(), pedidoId);
    }

    /**
     * Lo pagado sale de la caja de hoy: por cada pago se inserta una devoluciÃ³n con el
     * mismo monto y mÃ©todo en el corte abierto, aunque el cobro sea de un corte anterior
     * ya cerrado. Sin pagos no hay nada que devolver y no se exige caja abierta.
     */
    private void registrarDevolucion(long pedidoId) {
        BigDecimal pagado = jdbc.queryForObject(
            "select pag.total_pagado from barrocontrol.pedido p\n" + TOTALES + "where p.id = ?",
            BigDecimal.class, pedidoId);
        if (pagado == null || pagado.signum() == 0) return;
        Long corteCajaId = corteAbierto(pedidoId).orElseThrow(() -> new CorteCajaInvalidoException(
            "No hay un corte de caja abierto en esta sucursal. "
                + "Abre uno antes de cancelar un pedido con pagos, para descontar la devoluciÃ³n."));
        jdbc.update("""
            insert into barrocontrol.pago_pedido (id_pedido, id_corte_caja, monto, metodo_pago, fecha_hora, tipo_pago)
            select id_pedido, ?, monto, metodo_pago, current_timestamp, ?
            from barrocontrol.pago_pedido
            where id_pedido = ? and tipo_pago <> ?
            order by id
            """, corteCajaId, DEVOLUCION, pedidoId, DEVOLUCION);
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
                   tot.total, pag.total_pagado, tot.total - pag.total_pagado as saldo,
                   n.eslogan as negocio_eslogan
            from barrocontrol.pedido p
            join barrocontrol.cliente c on c.id = p.id_cliente
            join barrocontrol.sucursal s on s.id = p.id_sucursal
            join barrocontrol.negocio n on n.id = s.id_negocio
            """ + TOTALES + "where p.id = ?", (rs, i) -> cabecera(rs), pedidoId);
        if (cabeceras.isEmpty()) throw new PedidoNoEncontradoException();
        return cabeceras.get(0);
    }

    private Vista cabecera(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        var cliente = new Cliente(rs.getString("cliente_nombre"), rs.getString("telefono"), rs.getString("ciudad"),
            rs.getString("colonia"), rs.getString("calle"));
        var productos = jdbc.query("""
            select d.id_producto, p.nombre, d.cantidad, d.precio_unitario, d.desc_aplicado, d.subtotal, p.imagen_url
            from barrocontrol.detalle_pedido d join barrocontrol.producto p on p.id = d.id_producto
            where d.id_pedido = ? order by d.id_producto
            """, (r, i) -> new Detalle(r.getLong(1), r.getString(2), r.getInt(3), r.getBigDecimal(4),
                r.getBigDecimal(5), r.getBigDecimal(6), r.getString(7)), id);
        var pagos = jdbc.query("""
            select id, monto, metodo_pago, tipo_pago, fecha_hora from barrocontrol.pago_pedido
            where id_pedido = ? order by id
            """, (r, i) -> new Pago(r.getLong(1), r.getBigDecimal(2), r.getString(3), r.getString(4),
                r.getTimestamp(5).toInstant()), id);
        BigDecimal saldo = rs.getBigDecimal("saldo");
        return new Vista(id, rs.getLong("id_sucursal"), rs.getString("folio"),
            rs.getString("descripcion"), rs.getString("tipo_entrega"), rs.getString("descripcion_envio"),
            rs.getString("estado"), rs.getTimestamp("fecha_creacion").toInstant(), cliente, productos, pagos,
            rs.getBigDecimal("total"), rs.getBigDecimal("total_pagado"), saldo, saldo.signum() == 0,
            rs.getString("negocio_eslogan"));
    }

    private record FilaResumen(long id, long sucursalId, String tipoEntrega, String estado, Instant fechaCreacion,
                               String cliente, BigDecimal total, BigDecimal totalPagado, BigDecimal saldo) { }

    @Override
    public Pagina<Resumen> listar(long sucursalId, long usuarioId, EstadoPedido estado, int pagina, int tamanio) {
        comprobarAcceso(sucursalId, usuarioId);
        String filtroEstado = estado == null ? "" : " and p.estado = ?\n";
        var argumentos = new ArrayList<Object>();
        argumentos.add(sucursalId);
        if (estado != null) argumentos.add(estado.name());
        Long total = jdbc.queryForObject(
            "select count(*) from barrocontrol.pedido p where p.id_sucursal = ?" + filtroEstado,
            Long.class, argumentos.toArray());
        long totalElementos = total == null ? 0 : total;
        long desplazamiento = (long) pagina * tamanio;
        if (desplazamiento >= totalElementos) {
            return new Pagina<>(List.of(), pagina, tamanio, totalElementos);
        }
        argumentos.add(tamanio);
        argumentos.add(desplazamiento);
        String consulta = """
            select p.id, p.id_sucursal, p.tipo_entrega, p.estado, p.fecha_creacion, c.nombre,
                   tot.total, pag.total_pagado, tot.total - pag.total_pagado as saldo
            from barrocontrol.pedido p
            join barrocontrol.cliente c on c.id = p.id_cliente
            """ + TOTALES + """
            where p.id_sucursal = ?
            """ + filtroEstado + """
            order by p.fecha_creacion desc, p.id desc
            limit ? offset ?
            """;
        var filas = jdbc.query(consulta, (rs, i) -> new FilaResumen(rs.getLong(1), rs.getLong(2),
            rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant(), rs.getString(6),
            rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9)), argumentos.toArray());
        // Los productos de las filas de esta pÃ¡gina, en una sola consulta.
        var ids = filas.stream().map(FilaResumen::id).toList();
        var productos = new LinkedHashMap<Long, List<ResumenProducto>>();
        String consultaProductos = "select d.id_pedido, pr.nombre, d.cantidad\n"
            + "from barrocontrol.detalle_pedido d join barrocontrol.producto pr on pr.id = d.id_producto\n"
            + "where d.id_pedido in (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")\n"
            + "order by d.id_pedido, d.id_producto";
        jdbc.query(consultaProductos, rs -> {
            productos.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                .add(new ResumenProducto(rs.getString(2), rs.getInt(3)));
        }, ids.toArray());
        var resumenes = filas.stream().map(f -> new Resumen(f.id(), f.sucursalId(), f.tipoEntrega(), f.estado(),
            f.fechaCreacion(), f.cliente(), productos.getOrDefault(f.id(), List.of()), f.total(), f.totalPagado(),
            f.saldo(), f.saldo().signum() == 0)).toList();
        return new Pagina<>(resumenes, pagina, tamanio, totalElementos);
    }

    private void comprobarAccesoPedido(long sucursalId, long usuarioId) {
        if (sucursales.negocioAutorizado(sucursalId, usuarioId).isEmpty()) throw new PedidoNoEncontradoException();
    }
}
