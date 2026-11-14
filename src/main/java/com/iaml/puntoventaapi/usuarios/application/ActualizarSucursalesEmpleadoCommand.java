package com.iaml.puntoventaapi.usuarios.application;

import java.util.List;

public record ActualizarSucursalesEmpleadoCommand(
    Long negocioId,
    Long empleadoId,
    List<Long> sucursalIds
) {
}
