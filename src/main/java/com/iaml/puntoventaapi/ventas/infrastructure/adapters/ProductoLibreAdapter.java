package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.ProductoLibreContrato;
import com.iaml.puntoventaapi.ventas.domain.port.ProductoLibrePort;
import org.springframework.stereotype.Component;

@Component("ventasProductoLibreAdapter")
public class ProductoLibreAdapter implements ProductoLibrePort {

    private final ProductoLibreContrato catalogo;

    public ProductoLibreAdapter(ProductoLibreContrato catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public ProductoLibre obtenerOCrear(Long negocioId) {
        var libre = catalogo.obtenerOCrear(negocioId);
        return new ProductoLibre(libre.productoId(), libre.nombre());
    }
}
