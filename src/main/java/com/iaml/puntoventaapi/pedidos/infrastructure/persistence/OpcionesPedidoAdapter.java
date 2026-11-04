package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import com.iaml.puntoventaapi.pedidos.domain.port.OpcionesPedidoPort;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

@Repository
public class OpcionesPedidoAdapter implements OpcionesPedidoPort {
    private final JdbcTemplate jdbc;
    private final SucursalContrato sucursales;

    public OpcionesPedidoAdapter(JdbcTemplate jdbc, SucursalContrato sucursales) {
        this.jdbc = jdbc;
        this.sucursales = sucursales;
    }

    private long negocioAutorizado(long sucursalId, long usuarioId) {
        return sucursales.negocioAutorizado(sucursalId, usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Sucursal no disponible."));
    }

    @Override
    public List<Producto> productos(long sucursalId, long usuarioId) {
        long negocioId = negocioAutorizado(sucursalId, usuarioId);
        return jdbc.query("""
            select p.id, p.nombre, p.precio, d.cantidad_minima, d.cantidad_maxima, d.descuento
            from barrocontrol.producto p
            join barrocontrol.categoria c on c.id = p.id_categoria
            join barrocontrol.area a on a.id = c.id_area
            left join lateral (
                select descuento.cantidad_minima, descuento.cantidad_maxima, descuento.descuento
                from barrocontrol.descuento descuento
                join barrocontrol.descuento_producto dp on dp.id_descuento = descuento.id
                where dp.id_producto = p.id and descuento.activo = true
                order by descuento.id limit 1
            ) d on true
            where a.id_negocio = ? and p.activo = true and c.activo = true and a.activo = true
            order by p.nombre, p.id
            """, (rs, row) -> new Producto(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getObject(4, Integer.class), rs.getObject(5, Integer.class), rs.getBigDecimal(6)), negocioId);
    }

    @Override
    public List<Corte> cortesAbiertos(long sucursalId, long usuarioId) {
        negocioAutorizado(sucursalId, usuarioId);
        return jdbc.query("""
            select cc.id, cc.fecha_hora_apertura from barrocontrol.corte_caja cc
            join barrocontrol.caja c on c.id = cc.id_caja
            where c.id_sucursal = ? and cc.fecha_hora_cierre is null order by cc.fecha_hora_apertura desc
            """, (rs, row) -> new Corte(rs.getLong(1), rs.getTimestamp(2).toInstant().toString()), sucursalId);
    }

    @Override
    public java.util.Optional<SucursalAsignada> sucursalAsignada(long usuarioId) {
        return jdbc.query("""
            select s.id, s.nombre, n.id, n.nombre from barrocontrol.usuario u
            join barrocontrol.rol r on r.id = u.id_rol
            join barrocontrol.sucursal s on s.id = u.id_sucursal
            join barrocontrol.negocio n on n.id = s.id_negocio and n.id = u.id_negocio
            where u.id = ? and r.nombre = 'EMPLEADO' and n.activo = true
            """, (rs, row) -> new SucursalAsignada(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)), usuarioId)
            .stream().findFirst();
    }
}
