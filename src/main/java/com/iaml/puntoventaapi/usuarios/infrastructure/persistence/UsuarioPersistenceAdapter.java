package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositorioPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final EntityManager entityManager;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository usuarioJpaRepository, EntityManager entityManager) {
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreoNormalizado(correo).map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        RolEntity rol = entityManager.getReference(RolEntity.class, usuario.getRolId());
        return UsuarioMapper.toDomain(usuarioJpaRepository.save(UsuarioMapper.toEntity(usuario, rol)));
    }
}
