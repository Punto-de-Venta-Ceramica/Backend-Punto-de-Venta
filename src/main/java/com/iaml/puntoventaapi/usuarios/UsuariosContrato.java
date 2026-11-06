package com.iaml.puntoventaapi.usuarios;

import java.util.Optional;

public interface UsuariosContrato {

    boolean existePorId(Long usuarioId);

    Optional<CredencialesUsuario> buscarCredencialesPorCorreo(String correo);
}
