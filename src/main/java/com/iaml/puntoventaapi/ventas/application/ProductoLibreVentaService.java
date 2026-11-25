package com.iaml.puntoventaapi.ventas.application;

import com.iaml.puntoventaapi.ventas.domain.port.ProductoLibrePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Entrega al cajero el producto "Artículo artesanal" de su negocio, tras comprobar que la sucursal es suya. */
@Service
public class ProductoLibreVentaService {

    private final PrepararVentaService preparacion;
    private final ProductoLibrePort productoLibre;

    public ProductoLibreVentaService(PrepararVentaService preparacion, ProductoLibrePort productoLibre) {
        this.preparacion = preparacion;
        this.productoLibre = productoLibre;
    }

    @Transactional
    public ProductoLibrePort.ProductoLibre obtener(Long sucursalId, Long usuarioId) {
        return productoLibre.obtenerOCrear(preparacion.resolverNegocio(sucursalId, usuarioId));
    }
}
