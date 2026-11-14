package com.iaml.puntoventaapi.pedidos.api.consultar;

import com.iaml.puntoventaapi.shared.domain.Pagina;
import java.util.List;

/** pagina empieza en 0. Misma forma que la paginación del catálogo: contenido, pagina, totalElementos, ultima. */
public record PaginaPedidosResponse<T>(List<T> contenido, int pagina, long totalElementos, boolean ultima) {

    public static <T> PaginaPedidosResponse<T> from(Pagina<T> pagina) {
        return new PaginaPedidosResponse<>(pagina.contenido(), pagina.pagina(), pagina.totalElementos(),
            pagina.ultima());
    }
}
