package com.iaml.puntoventaapi.sucursal.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
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
        com.iaml.puntoventaapi.negocios.domain.Sucursal entity;
        if (sucursal.getId() == null) {
            Negocio negocio = entityManager.getReference(Negocio.class, sucursal.getNegocioId());
            entity = SucursalMapper.toEntity(sucursal, negocio);
        } else {
            entity = sucursales.findByIdAndNegocio_Usuarios_Id(sucursal.getId(), usuarioId)
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
        return sucursales.findByIdAndNegocio_Usuarios_Id(id, usuarioId).map(SucursalMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> buscarPorNegocioId(Long negocioId, Long usuarioId) {
        return sucursales.findByNegocio_IdAndNegocio_Usuarios_IdOrderByIdAsc(negocioId, usuarioId).stream()
            .map(SucursalMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        try {
            var entity = sucursales.findByIdAndNegocio_Usuarios_Id(id, usuarioId)
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

