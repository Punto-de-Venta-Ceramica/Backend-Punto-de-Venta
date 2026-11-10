package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
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
    public Map<Long, ProductoCotizado> cotizarProductos(Long negocioId, Map<Long, Integer> cantidadesPorProducto) {
        return catalogo.cotizarProductos(negocioId, cantidadesPorProducto).values().stream()
            .collect(Collectors.toUnmodifiableMap(p -> p.productoId(),
                p -> new ProductoCotizado(p.productoId(), p.nombre(), p.precioUnitario(), p.porcentajeDescuento())));
    }
}
