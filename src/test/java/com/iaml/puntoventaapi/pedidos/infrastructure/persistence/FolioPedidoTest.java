package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** El folio que ve el cliente es corto: PED- y el id con al menos 6 dígitos. */
class FolioPedidoTest {

    @Test
    void foliosCortosYLegibles() {
        assertEquals("PED-000007", PedidoPersistenceAdapter.folio(7));
        assertEquals("PED-000123", PedidoPersistenceAdapter.folio(123));
        assertEquals("PED-1234567", PedidoPersistenceAdapter.folio(1_234_567));
    }
}
