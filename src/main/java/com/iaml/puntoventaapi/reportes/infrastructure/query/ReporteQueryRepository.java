package com.iaml.puntoventaapi.reportes.infrastructure.query;

import com.iaml.puntoventaapi.reportes.application.port.ReporteConsultaPort;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Corte;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Cuadre;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.MedioPago;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Movimiento;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Producto;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Sucursal;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.TipoMovimiento;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Vendedor;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteQueryRepository implements ReporteConsultaPort {
    private static final String SUCURSAL = """
        select s.id, s.nombre, n.nombre as negocio
        from barrocontrol.sucursal s
        join barrocontrol.negocio n on n.id = s.id_negocio
        where s.id = :sucursalId
        """;

    private static final String CORTES = """
        select cc.id, concat_ws(' ', u.nombre, u.apellido_paterno, u.apellido_materno) as abierto_por,
               cc.fecha_hora_apertura, cc.fecha_hora_cierre, cc.monto_inicial,
               cc.total_efectivo, cc.total_real,
               coalesce(ventas.efectivo, 0) + coalesce(pedidos.efectivo, 0) as neto_efectivo
        from barrocontrol.corte_caja cc
        join barrocontrol.caja c on c.id = cc.id_caja
        join barrocontrol.usuario u on u.id = cc.id_usuario
        left join lateral (
            select sum(d.subtotal) as efectivo
            from barrocontrol.venta v
            join barrocontrol.detalle_venta d on d.id_venta = v.id
            where v.id_corte_caja = cc.id and upper(trim(v.metodo_pago)) = 'EFECTIVO'
        ) ventas on true
        left join lateral (
            select sum(case when upper(trim(pp.tipo_pago)) = 'DEVOLUCION' then -pp.monto else pp.monto end)
                   as efectivo
            from barrocontrol.pago_pedido pp
            where pp.id_corte_caja = cc.id and upper(trim(pp.metodo_pago)) = 'EFECTIVO'
              and upper(trim(pp.tipo_pago)) in ('ANTICIPO', 'LIQUIDACION', 'DEVOLUCION')
        ) pedidos on true
        where c.id_sucursal = :sucursalId
          and cc.fecha_hora_apertura < :hasta and cc.fecha_hora_cierre >= :desde
        order by cc.fecha_hora_apertura, cc.id
        """;

    private static final String MOVIMIENTOS = """
        with cortes as (
            select cc.id
            from barrocontrol.corte_caja cc
            join barrocontrol.caja c on c.id = cc.id_caja
            where c.id_sucursal = :sucursalId
              and cc.fecha_hora_apertura < :hasta and cc.fecha_hora_cierre >= :desde
        )
        select v.id, v.id_corte_caja, v.fecha_hora, 'VENTA' as tipo, upper(trim(v.metodo_pago)) as medio,
               concat('#', v.id) as referencia, u.id as vendedor_id,
               concat_ws(' ', u.nombre, u.apellido_paterno, u.apellido_materno) as vendedor,
               null::numeric as monto,
               d.id_producto as producto_id, coalesce(d.nombre_producto, p.nombre) as producto,
               d.cantidad, d.precio_unitario, d.desc_aplicado, d.subtotal
        from cortes cc
        join barrocontrol.venta v on v.id_corte_caja = cc.id
        join barrocontrol.usuario u on u.id = v.id_usuario
        join barrocontrol.detalle_venta d on d.id_venta = v.id
        join barrocontrol.producto p on p.id = d.id_producto
        where v.fecha_hora >= :desde and v.fecha_hora < :hasta
          and upper(trim(v.metodo_pago)) in ('EFECTIVO', 'TARJETA')
        union all
        select pp.id, pp.id_corte_caja, pp.fecha_hora, upper(trim(pp.tipo_pago)), upper(trim(pp.metodo_pago)),
               concat('Pedido ', pe.folio), null, null, pp.monto,
               d.id_producto, p.nombre, d.cantidad, d.precio_unitario, d.desc_aplicado, d.subtotal
        from cortes cc
        join barrocontrol.pago_pedido pp on pp.id_corte_caja = cc.id
        join barrocontrol.pedido pe on pe.id = pp.id_pedido
        left join barrocontrol.detalle_pedido d on d.id_pedido = pe.id
        left join barrocontrol.producto p on p.id = d.id_producto
        where pp.fecha_hora >= :desde and pp.fecha_hora < :hasta
          and upper(trim(pp.metodo_pago)) in ('EFECTIVO', 'TARJETA')
          and upper(trim(pp.tipo_pago)) in ('ANTICIPO', 'LIQUIDACION', 'DEVOLUCION')
        order by fecha_hora, id, tipo, producto_id
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public ReporteQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Sucursal> buscarSucursal(long sucursalId) {
        return jdbc.query(SUCURSAL, Map.of("sucursalId", sucursalId), (rs, fila) ->
            new Sucursal(rs.getLong("id"), rs.getString("nombre"), rs.getString("negocio")))
            .stream().findFirst();
    }

    @Override
    public List<CorteConsultado> buscarCortes(long sucursalId, Instant desde, Instant hastaExclusivo) {
        return jdbc.query(CORTES, parametros(sucursalId, desde, hastaExclusivo), (rs, fila) -> {
            long corteId = rs.getLong("id");
            BigDecimal fondo = rs.getBigDecimal("monto_inicial");
            BigDecimal esperado = rs.getBigDecimal("total_efectivo");
            BigDecimal contado = rs.getBigDecimal("total_real");
            var corte = new Corte(corteId, rs.getString("abierto_por"), instante(rs, "fecha_hora_apertura"),
                instante(rs, "fecha_hora_cierre"), fondo);
            var cuadre = new Cuadre(corteId, fondo, rs.getBigDecimal("neto_efectivo"), esperado, contado,
                contado.subtract(esperado));
            return new CorteConsultado(corte, cuadre);
        });
    }

    @Override
    public List<Movimiento> buscarMovimientos(long sucursalId, Instant desde, Instant hastaExclusivo) {
        return jdbc.query(MOVIMIENTOS, parametros(sucursalId, desde, hastaExclusivo),
            ReporteQueryRepository::leerMovimientos);
    }

    private static List<Movimiento> leerMovimientos(ResultSet rs) throws SQLException {
        var movimientos = new LinkedHashMap<ClaveMovimiento, Movimiento>();
        while (rs.next()) {
            var clave = new ClaveMovimiento(rs.getLong("id"), TipoMovimiento.valueOf(rs.getString("tipo")));
            Movimiento movimiento = movimientos.get(clave);
            if (movimiento == null) {
                movimiento = leerCabecera(rs, clave);
                movimientos.put(clave, movimiento);
            }
            Long productoId = rs.getObject("producto_id", Long.class);
            if (productoId != null) {
                movimiento.productos().add(new Producto(productoId, rs.getString("producto"), rs.getInt("cantidad"),
                    rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("desc_aplicado"), rs.getBigDecimal("subtotal")));
            }
        }
        return movimientos.values().stream().map(ReporteQueryRepository::completar).toList();
    }

    private static Movimiento leerCabecera(ResultSet rs, ClaveMovimiento clave) throws SQLException {
        Long vendedorId = rs.getObject("vendedor_id", Long.class);
        Vendedor vendedor = vendedorId == null ? null : new Vendedor(vendedorId, rs.getString("vendedor"));
        BigDecimal monto = rs.getBigDecimal("monto");
        return new Movimiento(clave.id(), rs.getLong("id_corte_caja"), instante(rs, "fecha_hora"), clave.tipo(),
            MedioPago.valueOf(rs.getString("medio")), rs.getString("referencia"), vendedor, monto, new ArrayList<>());
    }

    private static Movimiento completar(Movimiento fila) {
        BigDecimal importe = switch (fila.tipo()) {
            case VENTA -> fila.productos().stream().map(Producto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            case DEVOLUCION -> fila.importe().negate();
            case ANTICIPO, LIQUIDACION -> fila.importe();
        };
        return new Movimiento(fila.id(), fila.corteId(), fila.fechaHora(), fila.tipo(), fila.medioPago(),
            fila.referencia(), fila.vendedor(), importe, List.copyOf(fila.productos()));
    }

    private static Map<String, ?> parametros(long sucursalId, Instant desde, Instant hastaExclusivo) {
        return Map.of("sucursalId", sucursalId, "desde", desde.atOffset(ZoneOffset.UTC),
            "hasta", hastaExclusivo.atOffset(ZoneOffset.UTC));
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, OffsetDateTime.class).toInstant();
    }

    private record ClaveMovimiento(long id, TipoMovimiento tipo) {}
}
