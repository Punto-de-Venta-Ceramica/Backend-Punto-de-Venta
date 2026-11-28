package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import org.junit.jupiter.api.Test;

/** Parámetros de la búsqueda del listado: estado, folio, patrón del nombre, dígitos del teléfono, sucursal. */
class FiltrosListadoTest {

    @Test
    void sinBuscarSoloFiltraPorEstado() {
        assertArrayEquals(new Object[] {"PRODUCCION", null, null, null, 7L},
            PedidoPersistenceAdapter.filtrosListado(7, EstadoPedido.PRODUCCION, null));
        assertArrayEquals(new Object[] {null, null, null, null, 7L},
            PedidoPersistenceAdapter.filtrosListado(7, null, null));
    }

    @Test
    void elNombreVaSinAcentosNiMayusculasYSinComodines() {
        assertArrayEquals(new Object[] {null, "JOSÉ Peña", "jose pena", null, 7L},
            PedidoPersistenceAdapter.filtrosListado(7, null, "JOSÉ Peña"));
        assertArrayEquals(new Object[] {null, "50%_a\\", "50\\%\\_a\\\\", null, 7L},
            PedidoPersistenceAdapter.filtrosListado(7, null, "50%_a\\"));
    }

    @Test
    void soloLoQueParecTelefonoBuscaPorDigitos() {
        assertArrayEquals(new Object[] {null, "(222) 123-45", "(222) 123-45", "22212345", 7L},
            PedidoPersistenceAdapter.filtrosListado(7, null, "(222) 123-45"));
        // El folio tiene letras: se busca exacto, no como teléfono.
        assertArrayEquals(new Object[] {null, "PED-000123", "ped-000123", null, 7L},
            PedidoPersistenceAdapter.filtrosListado(7, null, "PED-000123"));
        assertArrayEquals(new Object[] {null, "--", "--", null, 7L},
            PedidoPersistenceAdapter.filtrosListado(7, null, "--"));
    }
}
