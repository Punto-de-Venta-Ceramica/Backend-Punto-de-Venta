package com.iaml.puntoventaapi.usuarios.domain.port;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import java.util.Optional;

public interface UsuarioRepositorioPort {

    boolean existePorId(Long usuarioId);

    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorId(Long usuarioId);

    Usuario guardar(Usuario usuario);
}
