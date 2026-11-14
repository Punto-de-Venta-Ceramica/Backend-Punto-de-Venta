package com.iaml.puntoventaapi.inventario.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventarioReglasExtraTest {

    @Test
    void desactivarYReactivarRespetanElEstado() {
        Inventario inventario = new Inventario(1L, 2L, 3L, 4);

        assertThrows(IllegalStateException.class, () -> inventario.reactivar(1));
        inventario.desactivar();
        assertFalse(inventario.isActivo());
        assertThrows(IllegalStateException.class, inventario::desactivar);

        inventario.reactivar(null);
        assertTrue(inventario.isActivo());
        assertEquals(4, inventario.getExistencia());

        inventario.desactivar();
        inventario.reactivar(9);
        assertEquals(9, inventario.getExistencia());
    }

    @Test
    void validaIdentificadoresPositivosYExistencia() {
        assertThrows(IllegalArgumentException.class, () -> new Inventario(1L, null, 3L, 0));
        assertThrows(IllegalArgumentException.class, () -> new Inventario(1L, 0L, 3L, 0));
        assertThrows(IllegalArgumentException.class, () -> new Inventario(1L, 2L, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new Inventario(1L, 2L, 0L, 0));
        assertThrows(IllegalArgumentException.class, () -> new Inventario(1L, 2L, 3L, -1));
        assertEquals(1L, new Inventario(1L, 2L, 3L, 0).getId());
    }

    @Test
    void ajustarValidaLosLimites() {
        Inventario inventario = new Inventario(1L, 2L, 3L, Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> inventario.ajustar(1));
        assertThrows(IllegalArgumentException.class, () -> inventario.ajustar(0));
        inventario.ajustar(-1);
        assertEquals(Integer.MAX_VALUE - 1, inventario.getExistencia());
    }
}
