package com.iaml.puntoventaapi.caja.application;

import com.iaml.puntoventaapi.caja.api.dto.ProductoVendidoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CalculadoraCorteTest {

    private CalculadoraCorte calculadora;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        calculadora = new CalculadoraCorte(jdbc);
    }

    @Test
    @DisplayName("Sin ventas ni pagos retorna ceros y monto inicial esperado")
    void sinMovimientosRetornaMontoInicialYCeros() {
        var resultado = calculadora.procesarCalculo(
            new BigDecimal("500.00"),
            List.of(),
            List.of(),
            List.of()
        );

        assertThat(resultado.totalVentas()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalAnticipos()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalLiquidaciones()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalCancelaciones()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalEfectivo()).isEqualByComparingTo("500.00");
        assertThat(resultado.totalTarjeta()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalEstimado()).isEqualByComparingTo("500.00");
        assertThat(resultado.productos()).isEmpty();
        assertThat(resultado.cancelacionesPedidos().detalles()).isEmpty();
    }

    @Test
    @DisplayName("Calcula ventas directas en efectivo y tarjeta con desglose de productos")
    void calculaVentasDirectas() {
        var ventas = List.of(
            new CalculadoraCorte.ItemVenta(1L, "Jarrón Barro", 2, new BigDecimal("150.00"), new BigDecimal("300.00"), "EFECTIVO"),
            new CalculadoraCorte.ItemVenta(1L, "Jarrón Barro", 1, new BigDecimal("150.00"), new BigDecimal("150.00"), "EFECTIVO"),
            new CalculadoraCorte.ItemVenta(2L, "Plato Decorativo", 3, new BigDecimal("80.00"), new BigDecimal("240.00"), "TARJETA")
        );

        var resultado = calculadora.procesarCalculo(
            new BigDecimal("200.00"),
            ventas,
            List.of(),
            List.of()
        );

        assertThat(resultado.totalVentas()).isEqualByComparingTo("690.00");
        assertThat(resultado.ventasEfectivo().totalVentas()).isEqualByComparingTo("450.00");
        assertThat(resultado.ventasTarjeta().totalVentas()).isEqualByComparingTo("240.00");
        assertThat(resultado.totalEfectivo()).isEqualByComparingTo("650.00"); // 200 + 450
        assertThat(resultado.totalTarjeta()).isEqualByComparingTo("240.00");
        assertThat(resultado.totalEstimado()).isEqualByComparingTo("890.00");

        // Agrupación de productos: Jarrón (2+1=3) y Plato (3)
        assertThat(resultado.productos()).hasSize(2);
        ProductoVendidoDto jarron = resultado.productos().stream()
            .filter(p -> p.idProducto().equals(1L)).findFirst().orElseThrow();
        assertThat(jarron.cantidad()).isEqualTo(3);
        assertThat(jarron.subtotal()).isEqualByComparingTo("450.00");

        ProductoVendidoDto plato = resultado.productos().stream()
            .filter(p -> p.idProducto().equals(2L)).findFirst().orElseThrow();
        assertThat(plato.cantidad()).isEqualTo(3);
        assertThat(plato.subtotal()).isEqualByComparingTo("240.00");
    }

    @Test
    @DisplayName("Descuenta automáticamente el anticipo de un pedido cancelado")
    void descuentaAnticipoDePedidoCancelado() {
        Instant ahora = Instant.now();
        var pagos = List.of(
            // Anticipo activo en efectivo
            new CalculadoraCorte.ItemPago(1L, 101L, "PED-001", "PENDIENTE", new BigDecimal("200.00"), "EFECTIVO", "ANTICIPO", ahora),
            // Liquidación en tarjeta
            new CalculadoraCorte.ItemPago(2L, 102L, "PED-002", "ENTREGADO", new BigDecimal("350.00"), "TARJETA", "LIQUIDACION", ahora),
            // Anticipo de pedido cancelado en efectivo
            new CalculadoraCorte.ItemPago(3L, 103L, "PED-003", "CANCELADO", new BigDecimal("100.00"), "EFECTIVO", "ANTICIPO", ahora)
        );

        var resultado = calculadora.procesarCalculo(
            new BigDecimal("1000.00"),
            List.of(),
            pagos,
            List.of()
        );

        // Cancelaciones
        assertThat(resultado.totalCancelaciones()).isEqualByComparingTo("100.00");
        assertThat(resultado.cancelacionesPedidos().cancelacionesEfectivo()).isEqualByComparingTo("100.00");
        assertThat(resultado.cancelacionesPedidos().detalles()).hasSize(1);
        assertThat(resultado.cancelacionesPedidos().detalles().get(0).folio()).isEqualTo("PED-003");

        // Anticipos (ingresaron 200 + 100 = 300)
        assertThat(resultado.totalAnticipos()).isEqualByComparingTo("300.00");
        assertThat(resultado.ventasEfectivo().anticipos()).isEqualByComparingTo("300.00");

        // Liquidaciones
        assertThat(resultado.totalLiquidaciones()).isEqualByComparingTo("350.00");
        assertThat(resultado.ventasTarjeta().liquidaciones()).isEqualByComparingTo("350.00");

        // Subtotal neto y total en efectivo:
        // Anticipos ingresados (300) - Cancelaciones salidas (100) = 200.00 neto
        // Monto inicial (1000) + Neto (200) = 1200.00
        assertThat(resultado.ventasEfectivo().subtotalNeto()).isEqualByComparingTo("200.00");
        assertThat(resultado.totalEfectivo()).isEqualByComparingTo("1200.00");

        // Tarjeta: Liquidación (350.00)
        assertThat(resultado.ventasTarjeta().subtotalNeto()).isEqualByComparingTo("350.00");
        assertThat(resultado.totalTarjeta()).isEqualByComparingTo("350.00");

        // Total estimado: 1200 + 350 = 1550.00
        assertThat(resultado.totalEstimado()).isEqualByComparingTo("1550.00");
    }

    @Test
    @DisplayName("Caso exacto: pedido liquidado y segundo pedido cancelado")
    void casoExactoUsuarioPedidoLiquidadoYPedidoCancelado() {
        Instant ahora = Instant.now();
        var pagos = List.of(
            // Pedido 1: anticipo 200 y liquidación 200 (Efectivo)
            new CalculadoraCorte.ItemPago(1L, 101L, "PED-01", "TERMINADO", new BigDecimal("200.00"), "EFECTIVO", "ANTICIPO", ahora),
            new CalculadoraCorte.ItemPago(2L, 101L, "PED-01", "TERMINADO", new BigDecimal("200.00"), "EFECTIVO", "LIQUIDACION", ahora),
            // Pedido 2: anticipo 200 (Efectivo) y luego cancelado
            new CalculadoraCorte.ItemPago(3L, 102L, "PED-02", "CANCELADO", new BigDecimal("200.00"), "EFECTIVO", "ANTICIPO", ahora)
        );

        var resultado = calculadora.procesarCalculo(
            new BigDecimal("100.00"), // Monto inicial
            List.of(),
            pagos,
            List.of()
        );

        // De anticipos entraron 400 (200 + 200)
        assertThat(resultado.totalAnticipos()).isEqualByComparingTo("400.00");
        assertThat(resultado.ventasEfectivo().anticipos()).isEqualByComparingTo("400.00");

        // De liquidación entraron 200
        assertThat(resultado.totalLiquidaciones()).isEqualByComparingTo("200.00");
        assertThat(resultado.ventasEfectivo().liquidaciones()).isEqualByComparingTo("200.00");

        // De cancelación salieron 200
        assertThat(resultado.totalCancelaciones()).isEqualByComparingTo("200.00");
        assertThat(resultado.ventasEfectivo().cancelaciones()).isEqualByComparingTo("200.00");

        // Subtotal neto: 400 + 200 - 200 = 400
        assertThat(resultado.ventasEfectivo().subtotalNeto()).isEqualByComparingTo("400.00");

        // Total esperado en caja: 100 (inicial) + 400 (neto) = 500
        assertThat(resultado.ventasEfectivo().totalEsperado()).isEqualByComparingTo("500.00");
        assertThat(resultado.totalEfectivo()).isEqualByComparingTo("500.00");
        assertThat(resultado.totalEstimado()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Combina ventas, anticipos, liquidaciones, cancelaciones y productos de pedidos")
    void escenarioCompletoConTodosLosCriterios() {
        Instant ahora = Instant.now();
        var ventas = List.of(
            new CalculadoraCorte.ItemVenta(10L, "Taza Cerámica", 5, new BigDecimal("60.00"), new BigDecimal("300.00"), "EFECTIVO"),
            new CalculadoraCorte.ItemVenta(20L, "Maceta Chica", 2, new BigDecimal("120.00"), new BigDecimal("240.00"), "TARJETA")
        );

        var pagos = List.of(
            new CalculadoraCorte.ItemPago(1L, 1001L, "FOL-01", "ENCARGADO", new BigDecimal("150.00"), "EFECTIVO", "ANTICIPO", ahora),
            new CalculadoraCorte.ItemPago(2L, 1002L, "FOL-02", "TERMINADO", new BigDecimal("200.00"), "TARJETA", "LIQUIDACION", ahora),
            new CalculadoraCorte.ItemPago(3L, 1003L, "FOL-03", "CANCELADO", new BigDecimal("80.00"), "EFECTIVO", "ANTICIPO", ahora),
            new CalculadoraCorte.ItemPago(4L, 1004L, "FOL-04", "CANCELADO", new BigDecimal("50.00"), "TARJETA", "ANTICIPO", ahora)
        );

        var productosPedido = List.of(
            new CalculadoraCorte.ItemPedidoProducto(30L, "Juego de Café", 1, new BigDecimal("400.00"), new BigDecimal("400.00"))
        );

        var resultado = calculadora.procesarCalculo(
            new BigDecimal("500.00"),
            ventas,
            pagos,
            productosPedido
        );

        // Ventas
        assertThat(resultado.totalVentas()).isEqualByComparingTo("540.00");
        assertThat(resultado.ventasEfectivo().totalVentas()).isEqualByComparingTo("300.00");
        assertThat(resultado.ventasTarjeta().totalVentas()).isEqualByComparingTo("240.00");

        // Anticipos (150 + 80 = 230 ef, 50 tj) -> total 280
        assertThat(resultado.totalAnticipos()).isEqualByComparingTo("280.00");
        assertThat(resultado.totalLiquidaciones()).isEqualByComparingTo("200.00");

        // Cancelaciones
        assertThat(resultado.totalCancelaciones()).isEqualByComparingTo("130.00");
        assertThat(resultado.cancelacionesPedidos().cancelacionesEfectivo()).isEqualByComparingTo("80.00");
        assertThat(resultado.cancelacionesPedidos().cancelacionesTarjeta()).isEqualByComparingTo("50.00");

        // Efectivo:
        // Monto inicial: 500.00
        // Ventas ef: 300.00 + Anticipos ef: 230.00 - Cancelaciones ef: 80.00 = Subtotal neto: 450.00
        // Total efectivo esperado: 500 + 450 = 950.00
        assertThat(resultado.ventasEfectivo().subtotalNeto()).isEqualByComparingTo("450.00");
        assertThat(resultado.totalEfectivo()).isEqualByComparingTo("950.00");

        // Tarjeta:
        // Ventas tj: 240.00 + Anticipos tj: 50.00 + Liquidaciones tj: 200.00 - Cancelaciones tj: 50.00 = Subtotal neto: 440.00
        // Total tarjeta esperado: 440.00
        assertThat(resultado.ventasTarjeta().subtotalNeto()).isEqualByComparingTo("440.00");
        assertThat(resultado.totalTarjeta()).isEqualByComparingTo("440.00");

        // Total Estimado: 950.00 + 440.00 = 1390.00
        assertThat(resultado.totalEstimado()).isEqualByComparingTo("1390.00");

        // Productos incluidos
        assertThat(resultado.productos()).hasSize(3);
    }
}

