package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import com.iaml.puntoventaapi.usuarios.domain.exception.CorreoYaRegistradoException;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositorioPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public boolean existePorId(Long usuarioId) {
        return usuarioJpaRepository.existsById(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreoNormalizado(correo).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long usuarioId) {
        return usuarioJpaRepository.findById(usuarioId).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        try {
            UsuarioEntity entity;
            if (usuario.getId() != null) {
                // Actualización: cargar entidad existente y actualizar sus campos
                entity = usuarioJpaRepository.findById(usuario.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuario.getId()));
                entity.setNombre(usuario.getNombre());
                entity.setApellidoPaterno(usuario.getApellidoPaterno());
                entity.setApellidoMaterno(usuario.getApellidoMaterno());
                entity.setCorreo(usuario.getCorreo());
                entity.setTelefono(usuario.getTelefono());
                entity.setPasswordHash(usuario.getPasswordHash());
                entity.setPasswordMustChange(usuario.isPasswordMustChange());
            } else {
                // Inserción nueva
                RolEntity rol = entityManager.getReference(RolEntity.class, usuario.getRolId());
                entity = UsuarioMapper.toEntity(usuario, rol);
            }
            return UsuarioMapper.toDomain(usuarioJpaRepository.saveAndFlush(entity));
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
}
