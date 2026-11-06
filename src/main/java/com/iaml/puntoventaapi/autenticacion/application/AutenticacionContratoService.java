package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.AutenticacionContrato;
import com.iaml.puntoventaapi.autenticacion.domain.port.GeneradorHashPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.RolRepositorioPort;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AutenticacionContratoService implements AutenticacionContrato {

    private final GeneradorHashPort generadorHash;
    private final RolRepositorioPort roles;

    public AutenticacionContratoService(GeneradorHashPort generadorHash, RolRepositorioPort roles) {
        this.generadorHash = generadorHash;
        this.roles = roles;
    }

    @Override
    public String hashearPassword(String passwordPlano) {
        return generadorHash.hashear(passwordPlano);
    }

    @Override
    public Optional<Long> buscarRolIdPorNombre(String nombre) {
        return roles.buscarIdPorNombre(nombre);
    }
}
