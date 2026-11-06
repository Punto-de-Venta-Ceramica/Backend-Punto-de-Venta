package com.iaml.puntoventaapi.autenticacion.domain.port;

public interface GeneradorHashPort {

    String hashear(String passwordPlano);
}
