package com.iaml.puntoventaapi.sucursal.infrastructure.adapters;

import com.iaml.puntoventaapi.sucursal.domain.port.NegocioPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NegocioAdapter implements NegocioPort {

    private final EntityManager entityManager;

    public NegocioAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return entityManager.createQuery("""
            select count(n) from Negocio n join n.usuarios u
            where n.id = :negocioId and u.id = :usuarioId
            """, Long.class)
            .setParameter("negocioId", negocioId)
            .setParameter("usuarioId", usuarioId)
            .getSingleResult() > 0;
    }
}

