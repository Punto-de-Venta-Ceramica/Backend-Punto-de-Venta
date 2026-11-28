package com.iaml.puntoventaapi.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iaml.puntoventaapi.caja.application.CalculadoraCorte.ItemPago;
import com.iaml.puntoventaapi.caja.application.CalculadoraCorte.ItemPedidoProducto;
import com.iaml.puntoventaapi.caja.application.CalculadoraCorte.ItemVenta;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.caja.infrastructure.persistence.CorteCajaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Casos límite que la base no produce pero el cálculo debe tolerar, y la carrera al bloquear el corte. */
class CajaBordesTest {

    private final CalculadoraCorte calculadora = new CalculadoraCorte(mock(JdbcTemplate.class));

    private static ItemPago pago(String monto, String metodo, String tipo) {
        return new ItemPago(1L, 1L, "PED-1", "ENCARGADO", monto == null ? null : new BigDecimal(monto), metodo, tipo,
            Instant.now());
    }

    @Test
    void trataLosImportesNulosComoCeroYNuncaEsperaCajaNegativa() {
        var resultado = calculadora.procesarCalculo(null,
            List.of(new ItemVenta(1L, "Jarrón", 1, null, null, "EFECTIVO")),
            List.of(pago(null, "EFECTIVO", "ANTICIPO"), pago("5", "EFECTIVO", "OTRO")),
            List.of(pago(null, "TARJETA", "ANTICIPO"), pago("40", "EFECTIVO", "ANTICIPO"),
                pago("30", "TARJETA", "LIQUIDACION")),
            List.of(new ItemPedidoProducto(1L, "Jarrón", 2, null, null)));

        assertThat(resultado.ventasEfectivo().montoInicial()).isEqualByComparingTo("0");
        assertThat(resultado.totalAnticipos()).isEqualByComparingTo("0");
        assertThat(resultado.totalCancelaciones()).isEqualByComparingTo("70");
        assertThat(resultado.totalEfectivo()).isEqualByComparingTo("0");
        assertThat(resultado.totalTarjeta()).isEqualByComparingTo("0");
        // La venta y el pedido quedan en renglones separados; el del pedido no lleva método.
        assertThat(resultado.productos()).extracting(p -> p.cantidad(), p -> p.metodoPago())
            .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "EFECTIVO"),
                org.assertj.core.groups.Tuple.tuple(2, null));
        assertThat(resultado.productos()).allSatisfy(p -> assertThat(p.precioUnitario()).isEqualByComparingTo("0"));
    }

    @Test
    void aceptaListasDeCancelacionesYProductosNulas() {
        var resultado = calculadora.procesarCalculo(new BigDecimal("10"), List.of(), List.of(), null, null);
        assertThat(resultado.totalEstimado()).isEqualByComparingTo("10");
        assertThat(resultado.productos()).isEmpty();
    }

    @Test
    void noEntregaElCorteSiSeCerroMientrasSeEsperabaElBloqueo() {
        var repositorio = mock(CorteCajaRepository.class);
        var cerrado = new CorteCaja();
        cerrado.setFechaHoraCierre(Instant.now());
        when(repositorio.buscarIdAbierto(7L)).thenReturn(Optional.of(3L));
        when(repositorio.buscarConBloqueo(3L)).thenReturn(Optional.of(cerrado));
        assertThat(new CajaContratoService(repositorio).bloquearCorteAbierto(7L)).isEmpty();
    }
}
