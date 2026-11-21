package com.iaml.puntoventaapi.ventas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Reglas del carrito y de la venta: ningún dato inválido llega a calcular importes. */
class VentaDominioTest {

    private static final BigDecimal CIEN = new BigDecimal("100.00");
    private static final List<ProductoSolicitado> UNO = List.of(new ProductoSolicitado(1L, 1, false));

    private static DetalleVenta detalle(long producto, String precio) {
        return new DetalleVenta(producto, "Jarrón", 1, new BigDecimal(precio), BigDecimal.ZERO);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rechazaProductosSinIdentificadorValido(Long productoId) {
        assertThatThrownBy(() -> new ProductoSolicitado(productoId, 1, false))
            .isInstanceOf(DatosVentaInvalidosException.class);
    }

    @Test
    void rechazaCantidadesQueNoSonPositivas() {
        assertThatThrownBy(() -> new ProductoSolicitado(1L, 0, false)).isInstanceOf(DatosVentaInvalidosException.class);
    }

    static Stream<Arguments> solicitudesInvalidas() {
        var conNulo = new ArrayList<ProductoSolicitado>(UNO);
        conNulo.add(null);
        return Stream.of(
            Arguments.of(null, MetodoPago.TARJETA, UNO, null, "sucursalId"),
            Arguments.of(0L, MetodoPago.TARJETA, UNO, null, "sucursalId"),
            Arguments.of(1L, MetodoPago.TARJETA, null, null, "productos"),
            Arguments.of(1L, MetodoPago.TARJETA, List.of(), null, "productos"),
            Arguments.of(1L, MetodoPago.TARJETA, Collections.nCopies(201, UNO.get(0)), null, "productos"),
            Arguments.of(1L, MetodoPago.TARJETA, conNulo, null, "productos"),
            Arguments.of(1L, MetodoPago.TARJETA, List.of(UNO.get(0), new ProductoSolicitado(1L, 2, false)), null, "productos"),
            Arguments.of(1L, MetodoPago.TARJETA, UNO, BigDecimal.ONE, "montoRecibido"),
            Arguments.of(1L, MetodoPago.EFECTIVO, UNO, new BigDecimal("-1"), "montoRecibido"),
            Arguments.of(1L, MetodoPago.EFECTIVO, UNO, new BigDecimal("1.001"), "montoRecibido"),
            Arguments.of(1L, MetodoPago.EFECTIVO, UNO, new BigDecimal("99999999999999999"), "montoRecibido"));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void rechazaCarritosInvalidos(Long sucursal, MetodoPago metodo, List<ProductoSolicitado> productos,
                                  BigDecimal monto, String campo) {
        assertThatThrownBy(() -> new SolicitudVenta(sucursal, metodo, productos, monto))
            .isInstanceOfSatisfying(DatosVentaInvalidosException.class, ex -> assertThat(ex.getErrores()).containsKey(campo));
    }

    @Test
    void aceptaEfectivoConMontoValido() {
        var solicitud = new SolicitudVenta(1L, MetodoPago.EFECTIVO, UNO, new BigDecimal("10.50"));
        assertThat(solicitud.cantidadesPorProducto()).containsEntry(1L, 1);
    }

    static Stream<Arguments> lineasInvalidas() {
        return Stream.of(
            Arguments.of(null, "Jarrón", 1, CIEN, BigDecimal.ZERO),
            Arguments.of(0L, "Jarrón", 1, CIEN, BigDecimal.ZERO),
            Arguments.of(1L, null, 1, CIEN, BigDecimal.ZERO),
            Arguments.of(1L, " ", 1, CIEN, BigDecimal.ZERO),
            Arguments.of(1L, "Jarrón", 0, CIEN, BigDecimal.ZERO),
            Arguments.of(1L, "Jarrón", 1, null, BigDecimal.ZERO),
            Arguments.of(1L, "Jarrón", 1, new BigDecimal("-1"), BigDecimal.ZERO),
            Arguments.of(1L, "Jarrón", 1, new BigDecimal("1.001"), BigDecimal.ZERO),
            Arguments.of(1L, "Jarrón", 1, new BigDecimal("1000000000000"), BigDecimal.ZERO),
            Arguments.of(1L, "Jarrón", 1, CIEN, null),
            Arguments.of(1L, "Jarrón", 1, CIEN, new BigDecimal("-1")),
            Arguments.of(1L, "Jarrón", 1, CIEN, new BigDecimal("100.01")),
            Arguments.of(1L, "Jarrón", 1, CIEN, new BigDecimal("1.001")),
            Arguments.of(1L, "Jarrón", 100_000, new BigDecimal("999999999999.99"), BigDecimal.ZERO));
    }

    @ParameterizedTest
    @MethodSource("lineasInvalidas")
    void rechazaLineasInvalidas(Long producto, String nombre, int cantidad, BigDecimal precio, BigDecimal descuento) {
        assertThatThrownBy(() -> new DetalleVenta(producto, nombre, cantidad, precio, descuento))
            .isInstanceOf(DatosVentaInvalidosException.class);
    }

    static Stream<Arguments> ventasInvalidas() {
        List<DetalleVenta> una = List.of(detalle(1, "100"));
        return Stream.of(
            Arguments.of(null, 1L, 1L, una), Arguments.of(0L, 1L, 1L, una),
            Arguments.of(1L, null, 1L, una), Arguments.of(1L, 0L, 1L, una),
            Arguments.of(1L, 1L, null, una), Arguments.of(1L, 1L, 0L, una),
            Arguments.of(1L, 1L, 1L, null), Arguments.of(1L, 1L, 1L, List.of()));
    }

    @ParameterizedTest
    @MethodSource("ventasInvalidas")
    void rechazaVentasSinDatosBasicos(Long usuario, Long negocio, Long sucursal, List<DetalleVenta> detalles) {
        assertThatThrownBy(() -> new Venta(usuario, negocio, sucursal, MetodoPago.TARJETA, detalles, null))
            .isInstanceOf(DatosVentaInvalidosException.class);
    }

    @Test
    void rechazaTotalesSobreElMaximoYEfectivoInsuficiente() {
        var enorme = new DetalleVenta(1L, "A", 10_000, new BigDecimal("999999999999.99"), BigDecimal.ZERO);
        var otra = new DetalleVenta(2L, "B", 10_000, new BigDecimal("999999999999.99"), BigDecimal.ZERO);
        assertThatThrownBy(() -> new Venta(1L, 1L, 1L, MetodoPago.TARJETA, List.of(enorme, otra), null))
            .isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> new Venta(1L, 1L, 1L, MetodoPago.EFECTIVO, List.of(detalle(1, "100")), BigDecimal.TEN))
            .isInstanceOfSatisfying(DatosVentaInvalidosException.class,
                ex -> assertThat(ex.getErrores()).containsKey("montoRecibido"));
    }

    @Test
    void laTarjetaNoDaCambio() {
        var venta = new Venta(1L, 1L, 1L, MetodoPago.TARJETA, List.of(detalle(1, "100")), null);
        assertThat(venta.cambio()).isNull();
        assertThat(venta.total()).isEqualByComparingTo("100");
    }
}
