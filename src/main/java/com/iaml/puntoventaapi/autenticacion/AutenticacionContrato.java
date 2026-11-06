package com.iaml.puntoventaapi.autenticacion;

import java.util.Optional;

public interface AutenticacionContrato {

    /** Devuelve el hash BCrypt con el que se guarda la contraseña. */
    String hashearPassword(String passwordPlano);

    Optional<Long> buscarRolIdPorNombre(String nombre);
}
