package com.iaml.puntoventaapi.usuarios.application;

public record AsignarSucursalCommand(
    Long negocioId,
    Long empleadoId,
    Long sucursalId
) {
}

