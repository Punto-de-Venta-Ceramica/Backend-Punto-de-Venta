package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.shared.domain.Pagina;
import java.util.List;

/** pagina empieza en 0. Solo lo que usa el cliente para paginar: total y si es la última. */
public record PaginaResponse<T>(List<T> contenido, int pagina, long totalElementos, boolean ultima) {

    public static <T> PaginaResponse<T> from(Pagina<T> pagina) {
        return new PaginaResponse<>(pagina.contenido(), pagina.pagina(), pagina.totalElementos(),
            pagina.ultima());
    }
}
