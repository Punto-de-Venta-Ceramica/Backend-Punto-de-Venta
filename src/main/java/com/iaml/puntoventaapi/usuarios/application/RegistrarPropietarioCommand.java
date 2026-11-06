package com.iaml.puntoventaapi.usuarios.application;

public record RegistrarPropietarioCommand(String nombre, String apellidoPaterno, String apellidoMaterno,
                                          String correo, String telefono, String password) {
}
