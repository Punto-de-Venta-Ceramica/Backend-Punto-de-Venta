package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.catalogo.ProductoParaVenta;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoJpaRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoContratoService implements CatalogoContrato {
    private final ProductoJpaRepository productos;
    private final DescuentoRepositorioPort descuentos;
    private final ProductoRepositorioPort productosVenta;

    public CatalogoContratoService(ProductoJpaRepository productos, DescuentoRepositorioPort descuentos,
                                   ProductoRepositorioPort productosVenta) {
        this.productos = productos;
        this.descuentos = descuentos;
        this.productosVenta = productosVenta;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoPedido> productoParaPedido(long productoId, long negocioId) {
        return productos.productoParaPedido(productoId, negocioId).map(producto -> {
            // Mismo criterio que la lista de productos del pedido: producto > categoría > área.
            var descuento = descuentos.buscarActivoPorProducto(productoId).orElse(null);
            return new ProductoPedido(producto.getPrecio(), descuento == null ? null : descuento.getCantidadMinima(),
                descuento == null ? null : descuento.getCantidadMaxima(),
                descuento == null ? null : descuento.getPorcentaje());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProductoParaVenta> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto) {
        if (cantidadesPorProducto.isEmpty()) {
            return Map.of();
        }
        List<Producto> activos = productosVenta.listarActivosPorIds(negocioId, cantidadesPorProducto.keySet());
        Map<Long, Descuento> porProducto = descuentos.buscarActivosPorProductos(
            activos.stream().map(Producto::getId).toList());
        Map<Long, ProductoParaVenta> resultado = new HashMap<>();
        for (Producto producto : activos) {
            Long productoId = producto.getId();
            Descuento descuento = porProducto.get(productoId);
            BigDecimal porcentaje = descuento == null ? BigDecimal.ZERO
                : descuento.porcentajeParaCantidad(cantidadesPorProducto.get(productoId));
            resultado.put(productoId,
                new ProductoParaVenta(productoId, producto.getNombre(), producto.getPrecio(), porcentaje));
        }
        return Map.copyOf(resultado);
    }
}
