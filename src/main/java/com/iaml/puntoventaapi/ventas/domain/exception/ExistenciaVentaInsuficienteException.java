package com.iaml.puntoventaapi.ventas.domain.exception;

public class ExistenciaVentaInsuficienteException extends RuntimeException {

    private final Long productoId;
    private final int cantidadSolicitada;
    private final int existenciaDisponible;

    public ExistenciaVentaInsuficienteException(Long productoId, int cantidadSolicitada, int existenciaDisponible) {
        super("La cantidad solicitada supera la existencia disponible del producto.");
        this.productoId = productoId;
        this.cantidadSolicitada = cantidadSolicitada;
        this.existenciaDisponible = existenciaDisponible;
    }

    public Long getProductoId() { return productoId; }
    public int getCantidadSolicitada() { return cantidadSolicitada; }
    public int getExistenciaDisponible() { return existenciaDisponible; }
}
