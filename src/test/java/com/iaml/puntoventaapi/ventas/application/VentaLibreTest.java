package com.iaml.puntoventaapi.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iaml.puntoventaapi.ventas.domain.DetalleVenta;
import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import com.iaml.puntoventaapi.ventas.domain.Venta;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.exception.ConflictoConfirmacionVentaException;
import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import com.iaml.puntoventaapi.ventas.domain.port.CajaPort;
import com.iaml.puntoventaapi.ventas.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.ventas.domain.port.InventarioPort;
import com.iaml.puntoventaapi.ventas.domain.port.SucursalPort;
import com.iaml.puntoventaapi.ventas.domain.port.VentaRepositorioPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Producto "Artículo artesanal": cobro libre con precio y nombre capturados en la venta. */
class VentaLibreTest {

    private static final long NORMAL = 2L;
    private static final long VARIOS = 3L;

    private final SucursalPort sucursales = mock(SucursalPort.class);
    private final CatalogoPort catalogo = mock(CatalogoPort.class);
    private final InventarioPort inventario = mock(InventarioPort.class);
    private final CajaPort caja = mock(CajaPort.class);
    private final VentaRepositorioPort ventas = mock(VentaRepositorioPort.class);
    private final PrepararVentaService preparacion = new PrepararVentaService(sucursales, catalogo, inventario);
    private final ConfirmarVentaService confirmacion = new ConfirmarVentaService(preparacion, caja, inventario, ventas);

    @BeforeEach
    void preparar() {
        when(sucursales.obtenerNegocioIdAccesible(1L, 9L)).thenReturn(Optional.of(5L));
        // El artículo artesanal no tiene fila de inventario: no aparece en las existencias.
        when(inventario.consultarExistencias(5L, 1L, 9L)).thenReturn(Map.of(NORMAL, 10));
        when(catalogo.cotizarProductos(any(), any())).thenReturn(Map.of(
            NORMAL, new ProductoCotizado(NORMAL, "Vaso", BigDecimal.TEN, BigDecimal.ZERO),
            VARIOS, new ProductoCotizado(VARIOS, "Artículo artesanal", BigDecimal.ZERO, BigDecimal.ZERO, true)));
        when(caja.bloquearCorteAbierto(1L)).thenReturn(Optional.of(new CajaPort.CorteAbierto(3L, 4L)));
        when(ventas.buscarPorOperacion(any(), any())).thenReturn(Optional.empty());
        when(ventas.guardar(any(), any(), any())).thenAnswer(a ->
            new VentaConfirmada(40L, "VTA-000040", 3L, 4L, Instant.now(), a.getArgument(0)));
    }

    private static ProductoSolicitado varios(int cantidad, String precio, String nombre) {
        return new ProductoSolicitado(VARIOS, cantidad, false, precio == null ? null : new BigDecimal(precio), nombre);
    }

    private static PrepararVentaCommand comando(ProductoSolicitado... productos) {
        return new PrepararVentaCommand(1L, MetodoPago.TARJETA, List.of(productos), null);
    }

    @Test
    void cotizaVariosConElPrecioYElNombreDelCajero() {
        var venta = preparacion.preparar(comando(varios(5, "10.00", "  Servilletas ")), 9L);

        var linea = venta.detalles().get(0);
        assertThat(linea.nombreProducto()).isEqualTo("Servilletas");
        assertThat(linea.precioUnitario()).isEqualByComparingTo("10.00");
        assertThat(linea.precioVariable()).isTrue();
        assertThat(venta.total()).isEqualByComparingTo("50.00");
    }

    @Test
    void sinNombreUsaArticuloArtesanal() {
        var venta = preparacion.preparar(comando(varios(1, "7.50", "   ")), 9L);
        assertThat(venta.detalles().get(0).nombreProducto()).isEqualTo("Artículo artesanal");
    }

    @Test
    void rechazaVariosSinPrecioConDescuentoOPrecioDemasiadoAlto() {
        assertThatThrownBy(() -> preparacion.preparar(comando(varios(1, null, null)), 9L))
            .isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> preparacion.preparar(comando(
            new ProductoSolicitado(VARIOS, 1, true, BigDecimal.TEN, null)), 9L))
            .isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> preparacion.preparar(comando(varios(1, "1000000.00", null)), 9L))
            .isInstanceOf(DatosVentaInvalidosException.class);
    }

    @Test
    void unProductoNormalNoAceptaPrecioNiNombrePropios() {
        assertThatThrownBy(() -> preparacion.preparar(comando(
            new ProductoSolicitado(NORMAL, 1, false, BigDecimal.ONE, null)), 9L))
            .isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> preparacion.preparar(comando(
            new ProductoSolicitado(NORMAL, 1, false, null, "Otro")), 9L))
            .isInstanceOf(DatosVentaInvalidosException.class);
    }

    @Test
    void laVentaSoloDeVariosNoTocaElInventario() {
        var confirmada = confirmacion.confirmar(comando(varios(5, "10.00", null)), 9L, UUID.randomUUID());

        assertThat(confirmada.venta().total()).isEqualByComparingTo("50.00");
        verify(inventario, never()).descontarParaVenta(any(), anyMap());
        verify(ventas).guardar(any(), any(), any());
    }

    @Test
    void laVentaMixtaDescuentaSoloLosProductosConInventario() {
        confirmacion.confirmar(comando(
            new ProductoSolicitado(NORMAL, 2, false), varios(5, "10.00", null)), 9L, UUID.randomUUID());

        verify(inventario).descontarParaVenta(1L, Map.of(NORMAL, 2));
    }

    @Test
    void elReintentoConOtroPrecioONombreEsOtraVenta() {
        UUID clave = UUID.randomUUID();
        var guardada = new Venta(9L, 5L, 1L, MetodoPago.TARJETA,
            List.of(new DetalleVenta(VARIOS, "Servilletas", 5, new BigDecimal("10.00"), BigDecimal.ZERO)), null);
        var anterior = new VentaConfirmada(40L, "VTA-000040", 3L, 4L, Instant.now(), guardada);
        when(ventas.buscarPorOperacion(1L, clave)).thenReturn(Optional.of(anterior));

        assertThat(confirmacion.confirmar(comando(varios(5, "10.00", "Servilletas")), 9L, clave)).isSameAs(anterior);
        assertThatThrownBy(() -> confirmacion.confirmar(comando(varios(5, "12.00", "Servilletas")), 9L, clave))
            .isInstanceOf(ConflictoConfirmacionVentaException.class);
        assertThatThrownBy(() -> confirmacion.confirmar(comando(varios(5, "10.00", "Bolsas")), 9L, clave))
            .isInstanceOf(ConflictoConfirmacionVentaException.class);
    }

    @Test
    void elDominioLimpiaElNombreYRechazaDatosInvalidos() {
        assertThat(varios(1, "1.00", "  ").nombre()).isNull();
        assertThatThrownBy(() -> varios(1, "1.00", "x".repeat(151))).isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> varios(1, "1.00", "a\nb")).isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> varios(1, "0", null)).isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> varios(1, "1.001", null)).isInstanceOf(DatosVentaInvalidosException.class);
    }

    private static ProductoSolicitado conManual(long producto, String precio, String porcentaje) {
        return new ProductoSolicitado(producto, 2, false, precio == null ? null : new BigDecimal(precio), null,
            new BigDecimal(porcentaje));
    }

    @Test
    void elDescuentoManualSeAplicaAProductosSinDescuentoYAlArtesanal() {
        // Vaso: 2 × 10 sin descuento de catálogo, 25 % manual → 15.00. Artesanal: 2 × 50 al 10 % → 90.00.
        var venta = preparacion.preparar(comando(conManual(NORMAL, null, "25"), conManual(VARIOS, "50.00", "10")), 9L);

        assertThat(venta.detalles().get(0).subtotal()).isEqualByComparingTo("15.00");
        assertThat(venta.detalles().get(1).subtotal()).isEqualByComparingTo("90.00");
    }

    @Test
    void elDescuentoManualReemplazaAlDelCatalogo() {
        when(catalogo.cotizarProductos(any(), any())).thenReturn(Map.of(
            NORMAL, new ProductoCotizado(NORMAL, "Vaso", BigDecimal.TEN, new BigDecimal("5.00"))));

        var venta = preparacion.preparar(comando(conManual(NORMAL, null, "100")), 9L);

        assertThat(venta.detalles().get(0).porcentajeDescuento()).isEqualByComparingTo("100");
        assertThat(venta.total()).isEqualByComparingTo("0.00");
    }

    @Test
    void rechazaDescuentoManualFueraDeRango() {
        for (String porcentaje : new String[] {"0.99", "100.01", "10.001", "-5"}) {
            assertThatThrownBy(() -> conManual(NORMAL, null, porcentaje))
                .isInstanceOf(DatosVentaInvalidosException.class);
        }
    }

    @Test
    void elReintentoConOtroDescuentoManualEsOtraVenta() {
        UUID clave = UUID.randomUUID();
        var guardada = new Venta(9L, 5L, 1L, MetodoPago.TARJETA,
            List.of(new DetalleVenta(NORMAL, "Vaso", 2, BigDecimal.TEN, new BigDecimal("25"))), null);
        var anterior = new VentaConfirmada(40L, "VTA-000040", 3L, 4L, Instant.now(), guardada);
        when(ventas.buscarPorOperacion(1L, clave)).thenReturn(Optional.of(anterior));

        assertThat(confirmacion.confirmar(comando(conManual(NORMAL, null, "25")), 9L, clave)).isSameAs(anterior);
        assertThatThrownBy(() -> confirmacion.confirmar(comando(conManual(NORMAL, null, "30")), 9L, clave))
            .isInstanceOf(ConflictoConfirmacionVentaException.class);
    }
}
