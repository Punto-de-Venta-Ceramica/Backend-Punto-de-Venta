package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.CorreoYaRegistradoException;
import com.iaml.puntoventaapi.usuarios.domain.exception.EmpleadoConDependenciasException;
import com.iaml.puntoventaapi.usuarios.domain.exception.EmpleadoNoEncontradoException;
import com.iaml.puntoventaapi.usuarios.domain.port.EmpleadoRepositorioPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmpleadoPersistenceAdapter implements EmpleadoRepositorioPort {

    private static final String ROL_EMPLEADO = "EMPLEADO";

    private final UsuarioJpaRepository usuarios;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Usuario guardar(Usuario empleado) {
        RolEntity rol = entityManager.getReference(RolEntity.class, empleado.getRolId());
        NegocioEntity negocio = entityManager.getReference(NegocioEntity.class, empleado.getNegocioId());
        SucursalEntity sucursal = empleado.getSucursalId() != null
            ? entityManager.getReference(SucursalEntity.class, empleado.getSucursalId())
            : null;

        try {
            UsuarioEntity entity = UsuarioMapper.toEntity(empleado, rol, negocio, sucursal);
            return UsuarioMapper.toDomain(usuarios.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uq_usuario_correo".equals(violation.getConstraintName())) {
                    throw new CorreoYaRegistradoException(ex);
                }
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarPorNegocio(Long negocioId) {
        return usuarios.findByNegocio_IdAndRol_NombreOrderByIdAsc(negocioId, ROL_EMPLEADO)
            .stream().map(UsuarioMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorIdYNegocio(Long empleadoId, Long negocioId) {
        return usuarios.findByIdAndNegocio_IdAndRol_Nombre(empleadoId, negocioId, ROL_EMPLEADO)
            .map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional
    public void actualizarSucursal(Long empleadoId, Long sucursalId) {
        UsuarioEntity entity = usuarios.findById(empleadoId)
            .orElseThrow(() -> new EmpleadoNoEncontradoException(empleadoId));
        SucursalEntity sucursal = sucursalId != null
            ? entityManager.getReference(SucursalEntity.class, sucursalId)
            : null;
        entity.setSucursal(sucursal);
        usuarios.saveAndFlush(entity);
    }

    @Override
    @Transactional
    public void actualizarDatos(Long empleadoId, String nombre, String apellidoPaterno, String apellidoMaterno) {
        UsuarioEntity entity = usuarios.findById(empleadoId)
            .orElseThrow(() -> new EmpleadoNoEncontradoException(empleadoId));
        if (nombre != null && !nombre.isBlank()) {
            entity.setNombre(nombre.trim());
        }
        if (apellidoPaterno != null) {
            entity.setApellidoPaterno(apellidoPaterno.trim().isEmpty() ? null : apellidoPaterno.trim());
        }
        if (apellidoMaterno != null) {
            entity.setApellidoMaterno(apellidoMaterno.trim().isEmpty() ? null : apellidoMaterno.trim());
        }
        usuarios.saveAndFlush(entity);
    }

    @Override
    @Transactional
    public void eliminar(Long empleadoId) {
        try {
            UsuarioEntity entity = usuarios.findById(empleadoId)
                .orElseThrow(() -> new EmpleadoNoEncontradoException(empleadoId));
            usuarios.delete(entity);
            usuarios.flush();
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                    throw new EmpleadoConDependenciasException(ex);
                }
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorCorreo(String correo) {
        return usuarios.findByCorreoNormalizado(correo).isPresent();
    }
}

