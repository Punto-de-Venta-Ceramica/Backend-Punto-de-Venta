package com.iaml.puntoventaapi.usuarios.domain.port;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import java.util.Optional;

public interface UsuarioRepositorioPort {

    boolean existePorId(Long usuarioId);

    Optional<Usuario> buscarPorCorreo(String correo);

    Usuario guardar(Usuario usuario);
}
