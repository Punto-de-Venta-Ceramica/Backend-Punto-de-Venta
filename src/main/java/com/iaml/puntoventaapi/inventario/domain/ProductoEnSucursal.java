package com.iaml.puntoventaapi.inventario.domain;

import java.math.BigDecimal;

public record ProductoEnSucursal(Long id, String nombre, BigDecimal precio, int existencia,
                                 String imagenUrl, Long areaId, String areaNombre,
                                 Long categoriaId, String categoriaNombre) { }
