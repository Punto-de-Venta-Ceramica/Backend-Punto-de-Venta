package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;

public record EmpleadoDetalle(
    Usuario usuario,
    String sucursalNombre,
    Boolean asignado
) {
}

