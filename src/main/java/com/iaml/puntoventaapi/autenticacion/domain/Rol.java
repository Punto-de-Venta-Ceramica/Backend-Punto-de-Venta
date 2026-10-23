package com.iaml.puntoventaapi.autenticacion.domain;

import java.util.Set;

public final class Rol {

    public static final String PROPIETARIO = "PROPIETARIO";
    public static final String EMPLEADO = "EMPLEADO";

    private final Long id;
    private final String nombre;
    private final Set<Permiso> permisos;

    public Rol(Long id, String nombre, Set<Permiso> permisos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El rol debe tener nombre");
        }
        this.id = id;
        this.nombre = nombre.trim().toUpperCase();
        this.permisos = permisos == null ? Set.of() : Set.copyOf(permisos);
    }

    public boolean esPropietario() {
        return PROPIETARIO.equals(nombre);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }
}
