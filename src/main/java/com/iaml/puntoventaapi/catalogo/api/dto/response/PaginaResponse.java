package com.iaml.puntoventaapi.catalogo.api.dto.response;

import com.iaml.puntoventaapi.shared.domain.Pagina;
import java.util.List;

/** pagina empieza en 0. */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamanio, long totalElementos,
                                int totalPaginas, boolean ultima) {

    public static <T> PaginaResponse<T> from(Pagina<T> pagina) {
        return new PaginaResponse<>(pagina.contenido(), pagina.pagina(), pagina.tamanio(),
            pagina.totalElementos(), pagina.totalPaginas(), pagina.ultima());
    }
}
