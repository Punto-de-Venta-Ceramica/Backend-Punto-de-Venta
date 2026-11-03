package com.iaml.puntoventaapi.usuarios.application;

public record RegistrarEmpleadoCommand(
    Long negocioId,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String correo,
    Long sucursalId
) {
}

