package com.iaml.puntoventaapi.negocios.domain;

import com.iaml.puntoventaapi.negocios.domain.exception.NegocioNoEncontradoException;
import java.time.Instant;
import java.util.Objects;

/** Estado y reglas del negocio, independientes de HTTP, Spring y JPA. */
public final class Negocio {

    private final Long id;
    private final String nombre;
    private final String logo;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;
    private final Instant fechaEliminacion;
    private final boolean activo;
    private final long version;

    public Negocio(Long id, String nombre, String logo, Instant fechaCreacion,
                   Instant fechaActualizacion, Instant fechaEliminacion, boolean activo, long version) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador del negocio debe ser positivo.");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > 150) {
            throw new IllegalArgumentException("El nombre del negocio es obligatorio y no debe superar 150 caracteres.");
        }
        if ((activo && fechaEliminacion != null) || (!activo && fechaEliminacion == null)) {
            throw new IllegalArgumentException("El estado del negocio y su fecha de eliminación no coinciden.");
        }
        if (version < 0) {
            throw new IllegalArgumentException("La versión del negocio no puede ser negativa.");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.logo = logo;
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria.");
        this.fechaActualizacion = Objects.requireNonNull(fechaActualizacion, "La fecha de actualización es obligatoria.");
        this.fechaEliminacion = fechaEliminacion;
        this.activo = activo;
        this.version = version;
    }

    public static Negocio crear(String nombre, String logo) {
        Instant ahora = Instant.now();
        return new Negocio(null, nombre, logo, ahora, ahora, null, true, 0);
    }

    public Negocio editar(String nombre, String logo) {
        if (!activo) {
            throw new NegocioNoEncontradoException();
        }
        return new Negocio(id, nombre, logo, fechaCreacion, Instant.now(), null, true, version);
    }

    public Negocio eliminar() {
        if (!activo) {
            return this;
        }
        Instant ahora = Instant.now();
        return new Negocio(id, nombre, logo, fechaCreacion, ahora, ahora, false, version);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getLogo() { return logo; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public Instant getFechaEliminacion() { return fechaEliminacion; }
    public boolean isActivo() { return activo; }
    public long getVersion() { return version; }
}
