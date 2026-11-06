package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import java.time.Instant;
import java.util.Objects;

/** Clasificación perteneciente a un área del catálogo. */
public final class Categoria {

    private final Long id;
    private final Long areaId;
    private final String nombre;
    private final String descripcion;
    private final boolean activo;
    private final Instant fechaEliminacion;

    public Categoria(Long id, Long areaId, String nombre, String descripcion) {
        this(id, areaId, nombre, descripcion, true, null);
    }

    public Categoria(Long id, Long areaId, String nombre, String descripcion,
                     boolean activo, Instant fechaEliminacion) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador de la categoría debe ser positivo.");
        }
        if (areaId == null || areaId <= 0) {
            throw new IllegalArgumentException("La categoría debe pertenecer a un área.");
        }
        DatosClasificacion datos = new DatosClasificacion(nombre, descripcion);
        if (activo != (fechaEliminacion == null)) {
            throw new IllegalArgumentException("El estado de la categoría y su fecha de eliminación no coinciden.");
        }
        this.id = id;
        this.areaId = areaId;
        this.nombre = datos.nombre();
        this.descripcion = datos.descripcion();
        this.activo = activo;
        this.fechaEliminacion = fechaEliminacion;
    }

    public Categoria editar(String nombre, String descripcion) {
        if (!activo) {
            throw new CatalogoNoEncontradoException("La categoría ya fue eliminada.");
        }
        return new Categoria(id, areaId, nombre, descripcion);
    }

    public Categoria eliminar(Instant fecha) {
        if (!activo) {
            return this;
        }
        return new Categoria(id, areaId, nombre, descripcion, false,
            Objects.requireNonNull(fecha, "La fecha de eliminación es obligatoria."));
    }

    public Long getId() {
        return id;
    }

    public Long getAreaId() {
        return areaId;
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

