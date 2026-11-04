package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.DescuentoJpaRepository;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoContratoService implements CatalogoContrato {
    private final ProductoJpaRepository productos;
    private final DescuentoJpaRepository descuentos;
    public CatalogoContratoService(ProductoJpaRepository productos, DescuentoJpaRepository descuentos) {
        this.productos = productos;
        this.descuentos = descuentos;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoPedido> productoParaPedido(long productoId, long negocioId) {
        return productos.productoParaPedido(productoId, negocioId).map(producto -> {
            var descuento = descuentos.activosPorProducto(productoId).stream().findFirst().orElse(null);
            return new ProductoPedido(producto.getPrecio(), descuento == null ? null : descuento.getCantidadMinima(),
                descuento == null ? null : descuento.getCantidadMaxima(), descuento == null ? null : descuento.getDescuento());
        });
    }
}
