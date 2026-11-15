package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.domain.port.AutoridadesVigentesPort;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AutoridadesVigentesAdapter implements AutoridadesVigentesPort {
    private final JdbcTemplate jdbc;
    public AutoridadesVigentesAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Autoridades> buscar(long usuarioId) {
        var filas = jdbc.query("""
            select r.nombre, p.nombre from barrocontrol.usuario u
            join barrocontrol.rol r on r.id = u.id_rol
            left join barrocontrol.rol_permiso rp on rp.id_rol = r.id
            left join barrocontrol.permiso p on p.id = rp.id_permiso
            where u.id = ?
            """, (rs, row) -> new Fila(rs.getString(1), rs.getString(2)), usuarioId);
        if (filas.isEmpty()) return Optional.empty();
        var permisos = filas.stream().map(Fila::permiso).filter(Objects::nonNull).distinct().toList();
        return Optional.of(new Autoridades(filas.get(0).rol(), permisos));
    }

    private record Fila(String rol, String permiso) { }
}
