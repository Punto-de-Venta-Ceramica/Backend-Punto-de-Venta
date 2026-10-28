package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalConDependenciasException;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalNoEncontradaException;
import com.iaml.puntoventaapi.sucursal.domain.port.SucursalRepositorioPort;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

@Component
public class SucursalPersistenceAdapter implements SucursalRepositorioPort {

    private final SucursalJpaRepository sucursales;
    private final EntityManager entityManager;

    public SucursalPersistenceAdapter(SucursalJpaRepository sucursales, EntityManager entityManager) {
        this.sucursales = sucursales;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Sucursal guardar(Sucursal sucursal, Long usuarioId) {
        SucursalEntity entity;
        if (sucursal.getId() == null) {
            NegocioEntity negocio = entityManager.getReference(NegocioEntity.class, sucursal.getNegocioId());
            entity = SucursalMapper.toEntity(sucursal, negocio);
        } else {
            entity = sucursales.findByIdAndNegocio_Usuarios_IdAndNegocio_ActivoTrue(sucursal.getId(), usuarioId)
                .orElseThrow(() -> new SucursalNoEncontradaException(sucursal.getId()));
            // PATCH: únicamente se modifican nombre y descripción.
            entity.setNombre(sucursal.getNombre());
            entity.setDescripcion(sucursal.getDescripcion());
        }
        return SucursalMapper.toDomain(sucursales.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sucursal> buscarPorId(Long id, Long usuarioId) {
        return sucursales.findByIdAndNegocio_Usuarios_IdAndNegocio_ActivoTrue(id, usuarioId).map(SucursalMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> buscarPorNegocioId(Long negocioId, Long usuarioId) {
        return sucursales.findByNegocio_IdAndNegocio_Usuarios_IdAndNegocio_ActivoTrueOrderByIdAsc(negocioId, usuarioId).stream()
            .map(SucursalMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        try {
            var entity = sucursales.findByIdAndNegocio_Usuarios_IdAndNegocio_ActivoTrue(id, usuarioId)
                .orElseThrow(() -> new SucursalNoEncontradaException(id));
            sucursales.delete(entity);
            sucursales.flush();
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                    throw new SucursalConDependenciasException(ex);
                }
            }
            throw ex;
        }
    }
}

