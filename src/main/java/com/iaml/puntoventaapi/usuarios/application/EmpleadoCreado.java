package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;

public record EmpleadoCreado(
    Usuario usuario,
    String passwordGenerada,
    String sucursalNombre
) {
}

