package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import java.math.BigDecimal;

/**
 * Filtros opcionales del listado de productos; null o texto vacío significa "sin filtro". El rango de precio
 * es inclusivo y cualquiera de sus extremos puede omitirse.
 */
public record FiltroProductos(Long areaId, Long categoriaId, String texto, Long sucursalId,
                              BigDecimal precioMinimo, BigDecimal precioMaximo) {

    public static final FiltroProductos SIN_FILTROS = new FiltroProductos(null, null, null, null);

    public FiltroProductos {
        texto = texto == null || texto.isBlank() ? null : texto.strip();
        if ((precioMinimo != null && precioMinimo.signum() < 0)
            || (precioMaximo != null && precioMaximo.signum() < 0)) {
            throw new ProductoInvalidoException("El rango de precio no puede ser negativo.");
        }
        if (precioMinimo != null && precioMaximo != null && precioMinimo.compareTo(precioMaximo) > 0) {
            throw new ProductoInvalidoException("El precio mínimo no puede ser mayor que el máximo.");
        }
    }

    public FiltroProductos(Long areaId, Long categoriaId, String texto, Long sucursalId) {
        this(areaId, categoriaId, texto, sucursalId, null, null);
    }
}
