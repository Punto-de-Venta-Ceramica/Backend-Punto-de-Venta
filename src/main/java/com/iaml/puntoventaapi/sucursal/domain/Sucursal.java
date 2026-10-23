package com.iaml.puntoventaapi.sucursal.domain;

/** Modelo de sucursal independiente de HTTP, Spring y JPA. */
public final class Sucursal {

    private final Long id;
    private final Long negocioId;
    private final String nombre;
    private final String descripcion;
    private final String ciudad;
    private final String calle;
    private final String codigoPostal;

    public Sucursal(Long id, Long negocioId, String nombre, String descripcion,
                    String ciudad, String calle, String codigoPostal) {
        if (negocioId == null || negocioId <= 0) {
            throw new IllegalArgumentException("El negocio debe tener un identificador positivo.");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > 150) {
            throw new IllegalArgumentException("El nombre es obligatorio y no debe superar 150 caracteres.");
        }
        this.id = id;
        this.negocioId = negocioId;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.calle = calle;
        this.codigoPostal = codigoPostal;
    }

    public Sucursal editar(String nombre, String descripcion) {
        if (nombre == null && descripcion == null) {
            throw new IllegalArgumentException("Envía al menos un campo editable: nombre o descripcion.");
        }
        return new Sucursal(id, negocioId, nombre == null ? this.nombre : nombre,
            descripcion == null ? this.descripcion : descripcion, ciudad, calle, codigoPostal);
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

    public String getCiudad() {
        return ciudad;
    }

    public String getCalle() {
        return calle;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }
}

