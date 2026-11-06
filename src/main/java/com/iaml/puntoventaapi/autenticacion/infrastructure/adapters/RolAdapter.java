package com.iaml.puntoventaapi.autenticacion.infrastructure.adapters;

import com.iaml.puntoventaapi.autenticacion.domain.port.RolRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Component
public class RolAdapter implements RolRepositorioPort {

    private final RolJpaRepository roles;

    public RolAdapter(RolJpaRepository roles) {
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarIdPorNombre(String nombre) {
        return roles.findByNombre(nombre).map(RolEntity::getId);
    }
}
