package com.iaml.puntoventaapi.inventario.domain;

import com.iaml.puntoventaapi.inventario.domain.exception.ExistenciaInsuficienteException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioTest {

    @Test
    void sumaExistencias() {
        Inventario inventario = new Inventario(1L, 2L, 3L, 10);

        inventario.ajustar(5);

        assertEquals(15, inventario.getExistencia());
    }

    @Test
    void restaExistencias() {
        Inventario inventario = new Inventario(1L, 2L, 3L, 10);

        inventario.ajustar(-10);

        assertEquals(0, inventario.getExistencia());
    }

    @Test
    void noPermiteQuedarPorDebajoDeCero() {
        Inventario inventario = new Inventario(1L, 2L, 3L, 4);

        assertThrows(ExistenciaInsuficienteException.class, () -> inventario.ajustar(-5));
        assertEquals(4, inventario.getExistencia());
    }

    @Test
    void rechazaAjusteEnCero() {
        Inventario inventario = new Inventario(1L, 2L, 3L, 4);

        assertThrows(IllegalArgumentException.class, () -> inventario.ajustar(0));
    }

    @Test
    void rechazaDesbordamientoDeLaExistencia() {
        Inventario inventario = new Inventario(1L, 2L, 3L, Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> inventario.ajustar(1));
        assertEquals(Integer.MAX_VALUE, inventario.getExistencia());
    }

    @Test
    void rechazaExistenciaInicialNegativaYReferenciasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> new Inventario(null, 2L, 3L, -1));
        assertThrows(IllegalArgumentException.class, () -> new Inventario(null, null, 3L, 0));
        assertThrows(IllegalArgumentException.class, () -> new Inventario(null, 2L, 0L, 0));
    }
}
