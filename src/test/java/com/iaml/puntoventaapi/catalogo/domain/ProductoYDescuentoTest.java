package com.iaml.puntoventaapi.catalogo.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductoYDescuentoTest {

    @Test
    void productoNormalizaElNombreYAceptaPrecioCero() {
        Producto producto = new Producto(null, 1L, "  Tarjeta  ", new BigDecimal("0.00"), true);

        assertEquals("Tarjeta", producto.getNombre());
    }

    @Test
    void productoRechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "  ", BigDecimal.TEN, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "x".repeat(151), BigDecimal.TEN, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "A", new BigDecimal("-0.01"), true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "A", new BigDecimal("1.234"), true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "A", null, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, null, "A", BigDecimal.TEN, true));
    }

    @Test
    void desactivarMarcaBajaLogica() {
        Producto producto = new Producto(5L, 1L, "A", BigDecimal.TEN, true);

        producto.desactivar();

        assertFalse(producto.isActivo());
    }

    @Test
    void productoAceptaImagenNulaODeCloudinaryHttps() {
        Producto sinImagen = new Producto(null, 1L, "Jarrón", BigDecimal.TEN, null, null, true);
        assertNull(sinImagen.getImagenUrl());
        assertNull(sinImagen.getImagenPublicId());

        Producto conImagen = new Producto(null, 1L, "Jarrón", BigDecimal.TEN,
            "  https://res.cloudinary.com/ecbb3tac/image/upload/v1/productos/jarron.jpg  ",
            "  productos/jarron  ", true);
        assertEquals("https://res.cloudinary.com/ecbb3tac/image/upload/v1/productos/jarron.jpg", conImagen.getImagenUrl());
        assertEquals("productos/jarron", conImagen.getImagenPublicId());
    }

    @Test
    void productoRechazaImagenUrlSinHttpsODeOtroDominio() {
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "Jarrón", BigDecimal.TEN,
            "http://res.cloudinary.com/ecbb3tac/image/upload/v1/jarron.jpg", "jarron", true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "Jarrón", BigDecimal.TEN,
            "https://otro-dominio.com/image/upload/v1/jarron.jpg", "jarron", true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "Jarrón", BigDecimal.TEN,
            "https://res.cloudinary.com.atacante.com/jarron.jpg", "jarron", true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "Jarrón", BigDecimal.TEN,
            "https://res.cloudinary.com/", "jarron", true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(null, 1L, "Jarrón", BigDecimal.TEN,
            "   ", "jarron", true));
    }

    @Test
    void descuentoAceptaRangoSinTope() {
        Descuento descuento = new Descuento(null, 1L, "Mayoreo", 12, null, new BigDecimal("10.00"), true);

        assertNull(descuento.getCantidadMaxima());
        assertEquals(12, descuento.getCantidadMinima());
    }

    @Test
    void descuentoRechazaRangosYPorcentajesInvalidos() {
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(null, 1L, "D", 0, null, BigDecimal.TEN, true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(null, 1L, "D", 10, 9, BigDecimal.TEN, true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(null, 1L, "D", 1, null, new BigDecimal("100.01"), true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(null, 1L, "D", 1, null, new BigDecimal("-1"), true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(null, 1L, " ", 1, null, BigDecimal.TEN, true));
    }

    @Test
    void modificarRevalidaLaReglaMaximaMayorOIgualQueMinima() {
        Descuento descuento = new Descuento(1L, 1L, "D", 6, 11, BigDecimal.TEN, true);

        assertThrows(IllegalArgumentException.class, () -> descuento.modificar(20, 10, BigDecimal.TEN));
        assertEquals(6, descuento.getCantidadMinima());

        descuento.modificar(20, 30, new BigDecimal("15.50"));
        assertEquals(20, descuento.getCantidadMinima());
        assertEquals(new BigDecimal("15.50"), descuento.getPorcentaje());
    }
}
