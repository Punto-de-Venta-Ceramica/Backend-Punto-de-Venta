package com.iaml.puntoventaapi.catalogo.domain;

import java.math.BigDecimal;

public final class Producto {

    private static final int NOMBRE_MAX = 150;
    private static final BigDecimal PRECIO_MAX = new BigDecimal("9999999999.99");

    private final Long id;
    private Long categoriaId;
    private String nombre;
    private BigDecimal precio;
    private boolean activo;

    public Producto(Long id, Long categoriaId, String nombre, BigDecimal precio, boolean activo) {
        this.id = id;
        setCategoriaId(categoriaId);
        setNombre(nombre);
        setPrecio(precio);
        this.activo = activo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        if (categoriaId == null || categoriaId <= 0) {
            throw new IllegalArgumentException("La categoría debe tener un identificador positivo.");
        }
        this.categoriaId = categoriaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > NOMBRE_MAX) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio y no debe superar " + NOMBRE_MAX + " caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0 || precio.scale() > 2 || precio.compareTo(PRECIO_MAX) > 0) {
            throw new IllegalArgumentException(
                "El precio es obligatorio, no puede ser negativo y admite máximo 2 decimales.");
        }
        this.precio = precio;
    }

    public boolean isActivo() {
        return activo;
    }
}
