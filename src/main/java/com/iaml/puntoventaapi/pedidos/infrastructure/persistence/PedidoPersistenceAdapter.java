package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.Locale;
import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.CorteCajaInvalidoException;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import com.iaml.puntoventaapi.shared.domain.Folio;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoPersistenceAdapter implements PedidoRepositorioPort, ConsultaPedidosPort {
    /**
     * Tipo de pago con el que se registra lo devuelto al cancelar un pedido. Va en el
     * corte abierto al cancelar, para descontarlo ahí aunque el cobro sea de otro corte.
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
            throw new AccessDeniedException("La sucursal no está disponible.");
        }
    }

    /**
     * Folio que ve el cliente: "PED-" y el id con al menos 6 dígitos (PED-000123). Es único porque el id lo
     * es. Los pedidos registrados antes de este cambio conservan su folio UUID.
     */
    static String folio(long pedidoId) {
        return Folio.PEDIDO.de(pedidoId);
    }

    @Override
    public long guardar(DatosRegistro datos, long clienteId, com.iaml.puntoventaapi.pedidos.domain.Pedido pedido) {
        long id = Objects.requireNonNull(jdbc.queryForObject("""
            insert into barrocontrol.pedido (id_sucursal, id_usuario, id_cliente, folio, descripcion,
                 tipo_entrega, estado, descripcion_envio, fecha_creacion, fecha_entrega)
            values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, ?) returning id
            """, Long.class, datos.sucursalId(), datos.usuarioId(), clienteId, UUID.randomUUID().toString(),
            datos.descripcion(), datos.tipoEntrega(), pedido.estado().name(), datos.descripcionEnvio(),
            Timestamp.from(datos.fechaEntrega())));
        // El UUID solo ocupa el campo obligatorio mientras se conoce el id; el folio visible es corto.
        jdbc.update("update barrocontrol.pedido set folio = ? where id = ?", folio(id), id);
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
    public void actualizarDatos(long pedidoId, long clienteId, DatosEdicion datos) {
        jdbc.update("""
            update barrocontrol.pedido set id_cliente = ?, descripcion = ?, tipo_entrega = ?, descripcion_envio = ?
            where id = ?
            """, clienteId, datos.descripcion(), datos.tipoEntrega(), datos.descripcionEnvio(), pedidoId);
    }

    @Override
    public void guardarPago(long pedidoId, PagoPedido pago) {
        BigDecimal saldo = Objects.requireNonNull(jdbc.queryForObject(
            "select tot.total - pag.total_pagado from barrocontrol.pedido p\n" + TOTALES + "where p.id = ?",
            BigDecimal.class, pedidoId));
        insertarPago(pedidoId, pago, pago.monto().compareTo(saldo) == 0);
    }

    private void insertarPago(long pedidoId, PagoPedido pago, boolean liquidado) {
        Long corteCajaId = corteAbierto(pedidoId).orElseThrow(CorteCajaInvalidoException::new);
        jdbc.update("""
            insert into barrocontrol.pago_pedido (id_pedido, id_corte_caja, monto, metodo_pago, fecha_hora, tipo_pago)
            values (?, ?, ?, ?, current_timestamp, ?)
            """, pedidoId, corteCajaId, pago.monto(), pago.metodoPago().name(), liquidado ? "LIQUIDACION" : "ANTICIPO");
    }

    /** Corte abierto de la sucursal del pedido, bloqueado hasta terminar la transacción. */
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
                -- Hasta entregarse guarda la fecha acordada; al entregarse, la real (una sola vez).
                fecha_entrega = case when ? = 'ENTREGADO' and estado <> 'ENTREGADO'
                    then current_timestamp else fecha_entrega end
            where id = ?
            """, estado.name(), estado.name(), pedidoId);
    }

    /**
     * Lo pagado sale de la caja de hoy: por cada pago se inserta una devolución con el
     * mismo monto y método en el corte abierto, aunque el cobro sea de un corte anterior
     * ya cerrado. Sin pagos no hay nada que devolver y no se exige caja abierta.
     */
    private void registrarDevolucion(long pedidoId) {
        BigDecimal pagado = Objects.requireNonNull(jdbc.queryForObject(
            "select pag.total_pagado from barrocontrol.pedido p\n" + TOTALES + "where p.id = ?",
            BigDecimal.class, pedidoId));
        if (pagado.signum() == 0) return;
        Long corteCajaId = corteAbierto(pedidoId).orElseThrow(() -> new CorteCajaInvalidoException(
            "No hay un corte de caja abierto en esta sucursal. "
                + "Abre uno antes de cancelar un pedido con pagos, para descontar la devolución."));
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
            return Objects.requireNonNull(
                jdbc.queryForObject("select id_sucursal from barrocontrol.pedido where id = ?", Long.class, pedidoId));
        } catch (EmptyResultDataAccessException ex) {
            throw new PedidoNoEncontradoException();
        }
    }

    @Override
    public Vista consultar(long pedidoId, long usuarioId) {
        comprobarAccesoPedido(sucursalDelPedido(pedidoId), usuarioId);
        // El pedido ya existe: sucursalDelPedido lo comprobó dentro de esta transacción.
        return jdbc.queryForObject("""
            select p.id, p.id_sucursal, p.folio, p.descripcion, p.tipo_entrega,
                   p.descripcion_envio, p.estado, p.fecha_creacion,
                   c.nombre as cliente_nombre, c.telefono, c.ciudad, c.colonia, c.calle,
                   tot.total, pag.total_pagado, tot.total - pag.total_pagado as saldo,
                   s.nombre as sucursal_nombre, n.id as negocio_id, n.nombre as negocio_nombre, p.fecha_entrega
            from barrocontrol.pedido p
            join barrocontrol.cliente c on c.id = p.id_cliente
            join barrocontrol.sucursal s on s.id = p.id_sucursal
            join barrocontrol.negocio n on n.id = s.id_negocio
            """ + TOTALES + "where p.id = ?", (rs, i) -> cabecera(rs), pedidoId);
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
            rs.getString("sucursal_nombre"), rs.getLong("negocio_id"), rs.getString("negocio_nombre"),
            rs.getTimestamp("fecha_entrega") == null ? null : rs.getTimestamp("fecha_entrega").toInstant());
    }

    /**
     * Filtros del listado, iguales para el conteo y la página (así "Mostrando X de Y" cuadra). Cada parámetro
     * se pasa una vez en f: estado, folio, patrón del nombre y dígitos del teléfono; null = sin ese filtro.
     */
    private static final String FILTROS_LISTADO = """
        cross join (select cast(? as varchar) as estado, cast(? as varchar) as folio,
                           cast(? as varchar) as nombre, cast(? as varchar) as digitos) f
        where p.id_sucursal = ? and (f.estado is null or p.estado = f.estado)
          and (f.folio is null
               or upper(p.folio) = upper(f.folio)
               or lower(translate(c.nombre, 'ÁÉÍÓÚÜÑáéíóúüñ', 'AEIOUUNaeiouun')) like '%' || f.nombre || '%'
               or (f.digitos is not null
                   and regexp_replace(coalesce(c.telefono, ''), '\\D', '', 'g') like '%' || f.digitos || '%'))
        """;

    private static final String CONTAR_PEDIDOS = """
        select count(*) from barrocontrol.pedido p
        join barrocontrol.cliente c on c.id = p.id_cliente
        """ + FILTROS_LISTADO;

    private static final String PAGINA_PEDIDOS = """
        select p.id, p.id_sucursal, p.tipo_entrega, p.estado, p.fecha_creacion, c.nombre,
               tot.total, pag.total_pagado, tot.total - pag.total_pagado as saldo
        from barrocontrol.pedido p
        join barrocontrol.cliente c on c.id = p.id_cliente
        """ + TOTALES + FILTROS_LISTADO + """
        order by p.fecha_creacion desc, p.id desc
        limit ? offset ?
        """;

    /** Parámetros de FILTROS_LISTADO, en su orden. busqueda ya viene recortada o null. */
    static Object[] filtrosListado(long sucursalId, EstadoPedido estado, String busqueda) {
        String nombre = null;
        String digitos = null;
        if (busqueda != null) {
            // Sin acentos ni mayúsculas, como el nombre en la consulta; %, _ y \ se buscan literales.
            nombre = Normalizer.normalize(busqueda, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("([\\\\%_])", "\\\\$1");
            // Solo si parece teléfono: "Ana" o "PED-000123" no deben coincidir con cualquier número.
            if (busqueda.matches("[\\d\\s()+.-]+")) {
                digitos = busqueda.replaceAll("\\D", "");
                if (digitos.isEmpty()) digitos = null;
            }
        }
        return new Object[] {estado == null ? null : estado.name(), busqueda, nombre, digitos, sucursalId};
    }

    private static final String PRODUCTOS_DE_PEDIDOS = """
        select d.id_pedido, pr.nombre, d.cantidad
        from barrocontrol.detalle_pedido d join barrocontrol.producto pr on pr.id = d.id_producto
        where d.id_pedido = any(?)
        order by d.id_pedido, d.id_producto
        """;

    private record FilaResumen(long id, long sucursalId, String tipoEntrega, String estado, Instant fechaCreacion,
                               String cliente, BigDecimal total, BigDecimal totalPagado, BigDecimal saldo) { }

    @Override
    public Pagina<Resumen> listar(long sucursalId, long usuarioId, EstadoPedido estado, String busqueda,
                                  int pagina, int tamanio) {
        comprobarAcceso(sucursalId, usuarioId);
        // Consultas fijas: los filtros opcionales van como parámetros (null = sin filtro).
        Object[] filtros = filtrosListado(sucursalId, estado, busqueda);
        long totalElementos = Objects.requireNonNull(jdbc.queryForObject(CONTAR_PEDIDOS, Long.class, filtros));
        long desplazamiento = (long) pagina * tamanio;
        if (desplazamiento >= totalElementos) {
            return new Pagina<>(List.of(), pagina, tamanio, totalElementos);
        }
        var filas = jdbc.query(PAGINA_PEDIDOS, (rs, i) -> new FilaResumen(rs.getLong(1), rs.getLong(2),
            rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant(), rs.getString(6),
            rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9)),
            filtros[0], filtros[1], filtros[2], filtros[3], filtros[4], tamanio, desplazamiento);
        // Los productos de las filas de esta página, en una sola consulta.
        Long[] ids = filas.stream().map(FilaResumen::id).toArray(Long[]::new);
        var productos = new LinkedHashMap<Long, List<ResumenProducto>>();
        RowCallbackHandler acumular = rs -> productos.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
            .add(new ResumenProducto(rs.getString(2), rs.getInt(3)));
        jdbc.query(con -> {
            var ps = con.prepareStatement(PRODUCTOS_DE_PEDIDOS);
            ps.setArray(1, con.createArrayOf("bigint", ids));
            return ps;
        }, acumular);
        var resumenes = filas.stream().map(f -> new Resumen(f.id(), f.sucursalId(), f.tipoEntrega(), f.estado(),
            f.fechaCreacion(), f.cliente(), productos.getOrDefault(f.id(), List.of()), f.total(), f.totalPagado(),
            f.saldo(), f.saldo().signum() == 0)).toList();
        return new Pagina<>(resumenes, pagina, tamanio, totalElementos);
    }

    private void comprobarAccesoPedido(long sucursalId, long usuarioId) {
        if (sucursales.negocioAutorizado(sucursalId, usuarioId).isEmpty()) throw new PedidoNoEncontradoException();
    }
}
