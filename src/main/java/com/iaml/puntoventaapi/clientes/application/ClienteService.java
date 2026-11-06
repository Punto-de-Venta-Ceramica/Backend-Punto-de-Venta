package com.iaml.puntoventaapi.clientes.application;

import com.iaml.puntoventaapi.clientes.ClientesContrato;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService implements ClientesContrato {
    private final JdbcTemplate jdbc;
    public ClienteService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public long registrarParaPedido(Datos datos) {
        Long id = jdbc.queryForObject("""
            insert into barrocontrol.cliente (nombre, telefono, ciudad, colonia, calle)
            values (?, ?, ?, ?, ?) returning id
            """, Long.class, datos.nombre(), datos.telefono(), datos.ciudad(), datos.colonia(), datos.calle());
        if (id == null) throw new IllegalStateException("No se pudo guardar el cliente.");
        return id;
    }
}
