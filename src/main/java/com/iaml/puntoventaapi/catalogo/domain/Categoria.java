package com.iaml.puntoventaapi.catalogo.domain;

/** Clasificación perteneciente a un área del catálogo. */
public final class Categoria {

    private final Long id;
    private final Long areaId;
    private final String nombre;
    private final String descripcion;

    public Categoria(Long id, Long areaId, String nombre, String descripcion) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador de la categoría debe ser positivo.");
        }
        if (areaId == null || areaId <= 0) {
            throw new IllegalArgumentException("La categoría debe pertenecer a un área.");
        }
        DatosClasificacion datos = new DatosClasificacion(nombre, descripcion);
        this.id = id;
        this.areaId = areaId;
        this.nombre = datos.nombre();
        this.descripcion = datos.descripcion();
    }

    public Categoria editar(String nombre, String descripcion) {
        return new Categoria(id, areaId, nombre, descripcion);
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
}

