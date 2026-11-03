package com.iaml.puntoventaapi.usuarios.application;

public record EditarEmpleadoCommand(
    Long negocioId,
    Long empleadoId,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno
) {
}

