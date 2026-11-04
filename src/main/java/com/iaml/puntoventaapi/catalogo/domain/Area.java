package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import java.time.Instant;
import java.util.Objects;

/** Clasificación del catálogo que pertenece a un solo negocio. */
public final class Area {

    private final Long id;
    private final Long negocioId;
    private final String nombre;
    private final String descripcion;
    private final boolean activo;
    private final Instant fechaEliminacion;

    public Area(Long id, Long negocioId, String nombre, String descripcion) {
        this(id, negocioId, nombre, descripcion, true, null);
    }

    public Area(Long id, Long negocioId, String nombre, String descripcion,
                boolean activo, Instant fechaEliminacion) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador del área debe ser positivo.");
        }
        if (negocioId == null || negocioId <= 0) {
            throw new IllegalArgumentException("El área debe pertenecer a un negocio.");
        }
        DatosClasificacion datos = new DatosClasificacion(nombre, descripcion);
        if (activo != (fechaEliminacion == null)) {
            throw new IllegalArgumentException("El estado del área y su fecha de eliminación no coinciden.");
        }
        this.id = id;
        this.negocioId = negocioId;
        this.nombre = datos.nombre();
        this.descripcion = datos.descripcion();
        this.activo = activo;
        this.fechaEliminacion = fechaEliminacion;
    }

    public Area editar(String nombre, String descripcion) {
        if (!activo) {
            throw new CatalogoNoEncontradoException("El área ya fue eliminada.");
        }
        return new Area(id, negocioId, nombre, descripcion);
    }

    public Area eliminar(Instant fecha) {
        if (!activo) {
            return this;
        }
        return new Area(id, negocioId, nombre, descripcion, false,
            Objects.requireNonNull(fecha, "La fecha de eliminación es obligatoria."));
    }

    public Long getId() {
        return id;
    }

    public Long getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getFechaEliminacion() {
        return fechaEliminacion;
    }
}

