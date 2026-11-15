package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.port.CategoriasParaRegistroPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriasParaRegistroAdapter implements CategoriasParaRegistroPort {
    private final JdbcTemplate jdbc;
    public CategoriasParaRegistroAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Categoria> listarPorNegocio(long negocioId) {
        return jdbc.query("""
            select c.id, c.nombre, a.id, a.nombre from barrocontrol.categoria c
            join barrocontrol.area a on a.id = c.id_area
            where a.id_negocio = ? and a.activo = true and c.activo = true
            order by a.nombre, c.nombre, c.id
            """, (rs, row) -> new Categoria(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)), negocioId);
    }
}
