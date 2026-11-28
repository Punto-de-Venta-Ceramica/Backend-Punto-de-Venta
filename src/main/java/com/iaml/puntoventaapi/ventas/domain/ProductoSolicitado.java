package com.iaml.puntoventaapi.ventas.domain;

import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * precioUnitario y nombre solo los admite el producto "Artículo artesanal" (cobro libre); el servicio los rechaza en
 * cualquier otro porque ahí es donde se conoce qué producto es.
 * descuentoManual: porcentaje (1 a 100) que captura el cajero; si viene, reemplaza al del catálogo.
 */
public record ProductoSolicitado(Long productoId, int cantidad, boolean aplicarDescuento,
                                 BigDecimal precioUnitario, String nombre, BigDecimal descuentoManual) {

    public static final int NOMBRE_MAX = 150;
    private static final BigDecimal CIEN = new BigDecimal("100");

    public ProductoSolicitado {
        if (productoId == null || productoId <= 0) {
            throw new DatosVentaInvalidosException(Map.of("productos", "Selecciona productos con identificadores válidos."));
        }
        if (cantidad <= 0) {
            throw new DatosVentaInvalidosException(Map.of("productos", "La cantidad de cada producto debe ser mayor que cero."));
        }
        if (precioUnitario != null && (precioUnitario.signum() <= 0 || precioUnitario.scale() > 2)) {
            throw new DatosVentaInvalidosException(Map.of("precioUnitario",
                "El precio debe ser mayor que cero y admitir máximo 2 decimales."));
        }
        if (descuentoManual != null && (descuentoManual.compareTo(BigDecimal.ONE) < 0
                || descuentoManual.compareTo(CIEN) > 0 || descuentoManual.scale() > 2)) {
            throw new DatosVentaInvalidosException(Map.of("descuentoManual",
                "El descuento debe estar entre 1 % y 100 % con máximo 2 decimales."));
        }
        if (nombre != null) {
            nombre = nombre.strip();
            if (nombre.length() > NOMBRE_MAX || nombre.codePoints().anyMatch(Character::isISOControl)) {
                throw new DatosVentaInvalidosException(Map.of("nombre",
                    "El nombre admite máximo " + NOMBRE_MAX + " caracteres sin saltos de línea."));
            }
            if (nombre.isEmpty()) {
                nombre = null;
            }
        }
    }

    public ProductoSolicitado(Long productoId, int cantidad, boolean aplicarDescuento,
                              BigDecimal precioUnitario, String nombre) {
        this(productoId, cantidad, aplicarDescuento, precioUnitario, nombre, null);
    }

    public ProductoSolicitado(Long productoId, int cantidad, boolean aplicarDescuento) {
        this(productoId, cantidad, aplicarDescuento, null, null, null);
    }
}
