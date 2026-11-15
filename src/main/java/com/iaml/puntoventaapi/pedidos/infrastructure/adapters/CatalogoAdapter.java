package com.iaml.puntoventaapi.pedidos.infrastructure.adapters;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.pedidos.domain.EscalonDescuento;
import com.iaml.puntoventaapi.pedidos.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.pedidos.domain.ProductoPedidoNoDisponibleException;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CatalogoAdapter implements CatalogoPort {
    private final CatalogoContrato catalogo;
    private final SucursalContrato sucursales;
    public CatalogoAdapter(CatalogoContrato catalogo, SucursalContrato sucursales) {
        this.catalogo = catalogo;
        this.sucursales = sucursales;
    }
    @Override
    public Producto consultar(long sucursalId, long usuarioId, long productoId) {
        long negocioId = sucursales.negocioAutorizado(sucursalId, usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Sucursal no disponible."));
        return catalogo.productoParaPedido(productoId, negocioId)
            .map(p -> new Producto(p.precio(), p.escalones().stream()
                .map(e -> new EscalonDescuento(e.cantidadMinima(), e.cantidadMaxima(), e.porcentaje()))
                .toList()))
            .orElseThrow(ProductoPedidoNoDisponibleException::new);
    }
}
