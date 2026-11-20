package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
import com.iaml.puntoventaapi.ventas.domain.RangoDescuentoVenta;
import java.util.Collection;
import java.util.List;
import com.iaml.puntoventaapi.ventas.domain.port.CatalogoPort;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component("ventasCatalogoAdapter")
public class CatalogoAdapter implements CatalogoPort {

    private final CatalogoContrato catalogo;

    public CatalogoAdapter(CatalogoContrato catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public Map<Long, List<RangoDescuentoVenta>> descuentosDisponibles(Collection<Long> productos) {
        return catalogo.descuentosParaVenta(productos).entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entrada -> entrada.getValue().stream()
                .map(rango -> new RangoDescuentoVenta(rango.cantidadMinima(), rango.cantidadMaxima(), rango.porcentaje()))
                .toList()));
    }

    @Override
    public Map<Long, ProductoCotizado> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto) {
        return catalogo.cotizarProductos(negocioId, cantidadesPorProducto).values().stream()
            .collect(Collectors.toUnmodifiableMap(p -> p.productoId(),
                p -> new ProductoCotizado(p.productoId(), p.nombre(), p.precioUnitario(), p.porcentajeDescuento())));
    }
}
