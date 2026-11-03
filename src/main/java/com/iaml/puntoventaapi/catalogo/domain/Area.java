package com.iaml.puntoventaapi.catalogo.domain;

/** Clasificación del catálogo que pertenece a un solo negocio. */
public final class Area {

    private final Long id;
    private final Long negocioId;
    private final String nombre;
    private final String descripcion;

    public Area(Long id, Long negocioId, String nombre, String descripcion) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador del área debe ser positivo.");
        }
        if (negocioId == null || negocioId <= 0) {
            throw new IllegalArgumentException("El área debe pertenecer a un negocio.");
        }
        DatosClasificacion datos = new DatosClasificacion(nombre, descripcion);
        this.id = id;
        this.negocioId = negocioId;
        this.nombre = datos.nombre();
        this.descripcion = datos.descripcion();
    }

    public Area editar(String nombre, String descripcion) {
        return new Area(id, negocioId, nombre, descripcion);
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
}

