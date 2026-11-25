package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.ventas.domain.port.ProductoLibrePort;

public record ProductoLibreResponse(Long productoId, String nombre) {

    public static ProductoLibreResponse from(ProductoLibrePort.ProductoLibre libre) {
        return new ProductoLibreResponse(libre.productoId(), libre.nombre());
    }
}
