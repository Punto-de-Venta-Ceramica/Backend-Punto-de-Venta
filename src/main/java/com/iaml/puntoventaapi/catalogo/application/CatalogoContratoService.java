package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoContratoService implements CatalogoContrato {
    private final ProductoJpaRepository productos;
    private final DescuentoRepositorioPort descuentos;
    public CatalogoContratoService(ProductoJpaRepository productos, DescuentoRepositorioPort descuentos) {
        this.productos = productos;
        this.descuentos = descuentos;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoPedido> productoParaPedido(long productoId, long negocioId) {
        return productos.productoParaPedido(productoId, negocioId).map(producto -> {
            // Mismo criterio que la lista de productos del pedido: el nivel producto > categoría > área que tenga descuentos.
            var escalones = descuentos.buscarEscalonesActivosPorProducto(productoId).stream()
                .map(d -> new EscalonDescuento(d.getCantidadMinima(), d.getCantidadMaxima(), d.getPorcentaje()))
                .toList();
            return new ProductoPedido(producto.getPrecio(), escalones);
        });
    }
}
