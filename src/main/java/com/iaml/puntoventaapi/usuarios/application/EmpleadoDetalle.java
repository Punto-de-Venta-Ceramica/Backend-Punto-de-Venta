package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import java.util.List;

public record EmpleadoDetalle(
    Usuario usuario,
    List<SucursalDetalle> sucursales,
    Boolean asignado
) {
}
