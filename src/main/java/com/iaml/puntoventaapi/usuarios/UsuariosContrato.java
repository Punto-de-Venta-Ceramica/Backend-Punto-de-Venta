package com.iaml.puntoventaapi.usuarios;

import java.util.Optional;

public interface UsuariosContrato {

    Optional<CredencialesUsuario> buscarCredencialesPorCorreo(String correo);
}
