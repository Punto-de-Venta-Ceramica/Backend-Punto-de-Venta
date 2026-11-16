package com.iaml.puntoventaapi.ventas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.iaml.puntoventaapi.shared.domain.Folio;
import org.junit.jupiter.api.Test;

/** El folio de venta tiene el mismo formato que el de pedido, pero con su propio prefijo. */
class FolioVentaTest {

    @Test
    void foliosCortosYLegibles() {
        assertEquals("VTA-000007", VentaPersistenceAdapter.folio(7));
        assertEquals("VTA-000123", VentaPersistenceAdapter.folio(123));
        assertEquals("VTA-1234567", VentaPersistenceAdapter.folio(1_234_567));
    }

    @Test
    void unaVentaYUnPedidoConElMismoIdNoSeConfunden() {
        assertThat(Folio.VENTA.de(9)).isNotEqualTo(Folio.PEDIDO.de(9));
    }
}
