package com.iaml.puntoventaapi.shared.domain;

import java.math.BigDecimal;

/** Filtros opcionales del catálogo paginado de una venta. */
public record FiltroProductosVenta(String buscar, Long areaId, Long categoriaId,
                                  BigDecimal precioMinimo, BigDecimal precioMaximo) { }
