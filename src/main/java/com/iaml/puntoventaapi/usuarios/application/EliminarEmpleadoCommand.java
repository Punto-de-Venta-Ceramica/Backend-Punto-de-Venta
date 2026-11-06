package com.iaml.puntoventaapi.usuarios.application;

public record EliminarEmpleadoCommand(
    Long negocioId,
    Long empleadoId
) {
}

