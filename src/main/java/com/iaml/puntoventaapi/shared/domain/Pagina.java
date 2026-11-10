package com.iaml.puntoventaapi.shared.domain;

import java.util.List;
import java.util.function.Function;

/** Página de resultados independiente de Spring Data: pagina empieza en 0. */
public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos) {

    public int totalPaginas() {
        return tamanio <= 0 ? 0 : (int) ((totalElementos + tamanio - 1) / tamanio);
    }

    public boolean ultima() {
        return pagina + 1 >= totalPaginas();
    }

    public <R> Pagina<R> mapear(Function<? super T, ? extends R> funcion) {
        return new Pagina<>(contenido.stream().<R>map(funcion).toList(), pagina, tamanio, totalElementos);
    }
}
