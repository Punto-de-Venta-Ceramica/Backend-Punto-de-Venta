package com.iaml.puntoventaapi.autenticacion.domain;

public record Permiso(Long id, String nombre) {

    public Permiso {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El permiso debe tener nombre");
        }
    }
}
