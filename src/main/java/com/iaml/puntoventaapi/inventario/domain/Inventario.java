package com.iaml.puntoventaapi.inventario.domain;

import com.iaml.puntoventaapi.inventario.domain.exception.ExistenciaInsuficienteException;

/**
 * Existencia de un producto en una sucursal.
 * Regla central: la existencia nunca puede ser negativa.
 */
public final class Inventario {

    private final Long id;
    private final Long sucursalId;
    private final Long productoId;
    private int existencia;
    private boolean activo;

    public Inventario(Long id, Long sucursalId, Long productoId, int existencia) {
        this(id, sucursalId, productoId, existencia, true);
    }

    /** Un registro inactivo significa que el producto se quitó de la sucursal; conserva su existencia. */
    public Inventario(Long id, Long sucursalId, Long productoId, int existencia, boolean activo) {
        if (sucursalId == null || sucursalId <= 0) {
            throw new IllegalArgumentException("La sucursal debe tener un identificador positivo.");
        }
        if (productoId == null || productoId <= 0) {
            throw new IllegalArgumentException("El producto debe tener un identificador positivo.");
        }
        this.id = id;
        this.sucursalId = sucursalId;
        this.productoId = productoId;
        setExistencia(existencia);
        this.activo = activo;
    }

    /** Quita el producto de la sucursal sin perder la existencia registrada. */
    public void desactivar() {
        if (!activo) {
            throw new IllegalStateException("El producto ya no está asignado a esta sucursal.");
        }
        this.activo = false;
    }

    /**
     * Vuelve a asignar el producto a la sucursal. Sin existencia indicada (null) conserva
     * la que tenía cuando se quitó.
     */
    public void reactivar(Integer existencia) {
        if (activo) {
            throw new IllegalStateException("El producto ya está asignado a esta sucursal.");
        }
        if (existencia != null) {
            setExistencia(existencia);
        }
        this.activo = true;
    }

    /** Suma (cantidad positiva) o resta (cantidad negativa) unidades a la existencia. */
    public void ajustar(int cantidad) {
        if (cantidad == 0) {
            throw new IllegalArgumentException("El ajuste debe ser distinto de cero.");
        }
        long resultado = (long) existencia + cantidad;
        if (resultado < 0) {
            throw new ExistenciaInsuficienteException();
        }
        if (resultado > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("La existencia resultante excede el máximo permitido.");
        }
        this.existencia = (int) resultado;
    }

    public Long getId() {
        return id;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getExistencia() {
        return existencia;
    }

    public void setExistencia(int existencia) {
        if (existencia < 0) {
            throw new IllegalArgumentException("La existencia no puede ser negativa.");
        }
        this.existencia = existencia;
    }
}
