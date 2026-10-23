package com.iaml.puntoventaapi.usuarios.domain.port;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import java.util.Optional;

public interface UsuarioRepositorioPort {

    Optional<Usuario> buscarPorCorreo(String correo);
}
