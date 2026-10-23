package com.iaml.puntoventaapi.usuarios.infrastructure.persistence;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositorioPort {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreoNormalizado(correo).map(UsuarioMapper::toDomain);
    }
}
