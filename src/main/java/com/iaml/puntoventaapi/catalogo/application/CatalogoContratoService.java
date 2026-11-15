package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.catalogo.ProductoParaVenta;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoJpaRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.Comparator;
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
            // Mismo criterio que la lista de productos del pedido: el nivel producto > categoría > área que tenga descuentos.
            var escalones = descuentos.buscarEscalonesActivosPorProducto(productoId).stream()
                .map(d -> new EscalonDescuento(d.getCantidadMinima(), d.getCantidadMaxima(), d.getPorcentaje()))
                .toList();
            return new ProductoPedido(producto.getPrecio(), escalones);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<EscalonDescuento>> descuentosParaVenta(Collection<Long> productoIds) {
        return descuentos.buscarEscalonesActivosPorProductos(productoIds).entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entrada -> entrada.getValue().stream()
                .filter(d -> d.getPorcentaje().signum() > 0)
                .map(d -> new EscalonDescuento(d.getCantidadMinima(), d.getCantidadMaxima(), d.getPorcentaje()))
                .toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProductoParaVenta> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto) {
        if (cantidadesPorProducto.isEmpty()) {
            return Map.of();
        }
        List<Producto> activos = productosVenta.listarActivosPorIds(negocioId, cantidadesPorProducto.keySet());
        Map<Long, List<Descuento>> porProducto = descuentos.buscarEscalonesActivosPorProductos(
            activos.stream().map(Producto::getId).toList());
        Map<Long, ProductoParaVenta> resultado = new HashMap<>();
        for (Producto producto : activos) {
            Long productoId = producto.getId();
            int cantidad = cantidadesPorProducto.get(productoId);
            BigDecimal porcentaje = porProducto.getOrDefault(productoId, List.of()).stream()
                .filter(descuento -> descuento.aplicaA(cantidad))
                .max(Comparator.comparing(Descuento::getCantidadMinima))
                .map(Descuento::getPorcentaje)
                .orElse(BigDecimal.ZERO);
            resultado.put(productoId,
                new ProductoParaVenta(productoId, producto.getNombre(), producto.getPrecio(), porcentaje));
        }
        return Map.copyOf(resultado);
    }
}
