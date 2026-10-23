package com.iaml.puntoventaapi.autenticacion.domain.port;

public interface PasswordHasherPort {

    boolean coincide(String passwordPlano, String passwordHash);
}
