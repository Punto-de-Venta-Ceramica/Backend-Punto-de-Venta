package com.iaml.puntoventaapi.ventas.api.dto;

import com.iaml.puntoventaapi.shared.domain.Pagina;
import com.iaml.puntoventaapi.ventas.domain.ProductoDisponible;
import java.util.List;

public record PaginaProductosVentaResponse(List<ProductoDisponibleResponse> contenido, int pagina,
                                          long totalElementos, boolean ultima) {
    public static PaginaProductosVentaResponse from(Pagina<ProductoDisponible> pagina) {
        return new PaginaProductosVentaResponse(pagina.mapear(ProductoDisponibleResponse::from).contenido(),
            pagina.pagina(), pagina.totalElementos(), pagina.pagina() >= pagina.totalPaginas() - 1);
    }
}
