package com.iaml.puntoventaapi.ventas.domain.exception;

public class DescuentoVentaNoDisponibleException extends RuntimeException {
    private final Long productoId;

    public DescuentoVentaNoDisponibleException(Long productoId) {
        super("El descuento del producto ya no está disponible para la cantidad seleccionada.");
        this.productoId = productoId;
    }

    public Long getProductoId() {
        return productoId;
    }
}
