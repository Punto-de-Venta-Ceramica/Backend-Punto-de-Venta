package com.iaml.puntoventaapi.autenticacion.application;

public record IniciarSesionCommand(String correo, String password) {
}
