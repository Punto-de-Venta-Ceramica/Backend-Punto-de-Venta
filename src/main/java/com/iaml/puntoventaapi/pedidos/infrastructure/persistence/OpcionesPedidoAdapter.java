package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import com.iaml.puntoventaapi.pedidos.domain.EscalonDescuento;
import com.iaml.puntoventaapi.pedidos.domain.port.OpcionesPedidoPort;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        // Una sola consulta: los productos activos del negocio y, en la misma fila, cada escalón vigente.
        Map<Long, Producto> productos = new LinkedHashMap<>();
        jdbc.query("""
            with productos as (
                select p.id, p.nombre, p.precio, p.id_categoria, c.id_area
                from barrocontrol.producto p
                join barrocontrol.categoria c on c.id = p.id_categoria
                join barrocontrol.area a on a.id = c.id_area
                where a.id_negocio = ? and p.activo = true and c.activo = true and a.activo = true
            ), candidatos as (
                -- Escalones de cada producto: todos los descuentos activos del nivel con mayor prioridad
                -- (asignados al producto, si no a su categoría, si no a su área).
                -- Debe coincidir con DescuentoPersistenceAdapter.buscarEscalonesActivosPorProducto.
                select pr.id as producto, 1 as prioridad, d.id, d.cantidad_minima, d.cantidad_maxima, d.descuento
                from productos pr
                join barrocontrol.descuento_producto dp on dp.id_producto = pr.id
                join barrocontrol.descuento d on d.id = dp.id_descuento and d.activo = true
                union all
                select pr.id, 2, d.id, d.cantidad_minima, d.cantidad_maxima, d.descuento
                from productos pr
                join barrocontrol.descuento_categoria dc on dc.id_categoria = pr.id_categoria
                join barrocontrol.descuento d on d.id = dc.id_descuento and d.activo = true
                union all
                select pr.id, 3, d.id, d.cantidad_minima, d.cantidad_maxima, d.descuento
                from productos pr
                join barrocontrol.descuento_area da on da.id_area = pr.id_area
                join barrocontrol.descuento d on d.id = da.id_descuento and d.activo = true
            ), escalones as (
                select x.* from (
                    select candidatos.*, min(prioridad) over (partition by producto) as mejor from candidatos
                ) x
                where x.prioridad = x.mejor
            )
            select pr.id, pr.nombre, pr.precio, e.cantidad_minima, e.cantidad_maxima, e.descuento
            from productos pr
            left join escalones e on e.producto = pr.id
            order by pr.nombre, pr.id, e.cantidad_minima, e.id
            """, rs -> {
                long id = rs.getLong(1);
                Producto producto = productos.get(id);
                if (producto == null) {
                    producto = new Producto(id, rs.getString(2), rs.getBigDecimal(3), new ArrayList<>());
                    productos.put(id, producto);
                }
                Integer minimo = rs.getObject(4, Integer.class);
                if (minimo != null) {
                    producto.descuentos().add(new EscalonDescuento(minimo, rs.getObject(5, Integer.class),
                        rs.getBigDecimal(6)));
                }
            }, negocioId);
        return productos.values().stream()
            .map(p -> new Producto(p.id(), p.nombre(), p.precio(), List.copyOf(p.descuentos())))
            .toList();
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
    public List<SucursalAsignada> sucursalesAsignadas(long usuarioId) {
        return jdbc.query("""
            select s.id, s.nombre, n.id, n.nombre from barrocontrol.usuario u
            join barrocontrol.rol r on r.id = u.id_rol
            join barrocontrol.empleado_sucursal es on es.id_usuario = u.id
            join barrocontrol.sucursal s on s.id = es.id_sucursal
            join barrocontrol.negocio n on n.id = s.id_negocio and n.id = u.id_negocio
            where u.id = ? and r.nombre = 'EMPLEADO' and n.activo = true
            order by s.nombre, s.id
            """, (rs, row) -> new SucursalAsignada(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)), usuarioId);
    }
}
