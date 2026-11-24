package com.iaml.puntoventaapi.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iaml.puntoventaapi.inventario.InventarioContrato;
import com.iaml.puntoventaapi.ventas.domain.DetalleVenta;
import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import com.iaml.puntoventaapi.ventas.domain.Venta;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import com.iaml.puntoventaapi.ventas.domain.exception.ExistenciaVentaInsuficienteException;
import com.iaml.puntoventaapi.ventas.domain.exception.RecursoVentaNoEncontradoException;
import com.iaml.puntoventaapi.ventas.domain.port.CajaPort;
import com.iaml.puntoventaapi.ventas.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.ventas.domain.port.InventarioPort;
import com.iaml.puntoventaapi.ventas.domain.port.SucursalPort;
import com.iaml.puntoventaapi.ventas.domain.port.VentaRepositorioPort;
import com.iaml.puntoventaapi.ventas.infrastructure.adapters.InventarioAdapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Lo que solo pasa con peticiones simultáneas o llamadas directas al servicio, sin pasar por la API. */
class VentasConcurrenciaTest {

    private final SucursalPort sucursales = mock(SucursalPort.class);
    private final CatalogoPort catalogo = mock(CatalogoPort.class);
    private final InventarioPort inventario = mock(InventarioPort.class);
    private final PrepararVentaService preparacion = new PrepararVentaService(sucursales, catalogo, inventario);

    @Test
    void validaLaSucursalAunqueSeLlameSinPasarPorLaApi() {
        assertThatThrownBy(() -> preparacion.resolverNegocio(null, 1L)).isInstanceOf(DatosVentaInvalidosException.class);
        assertThatThrownBy(() -> preparacion.resolverNegocio(0L, 1L)).isInstanceOf(DatosVentaInvalidosException.class);
    }

    @Test
    void unProductoDelCatalogoSinInventarioEnLaSucursalNoSePuedeVender() {
        when(sucursales.obtenerNegocioIdAccesible(1L, 9L)).thenReturn(Optional.of(5L));
        when(inventario.consultarExistencias(5L, 1L, 9L)).thenReturn(Map.of());
        when(catalogo.cotizarProductos(any(), any())).thenReturn(Map.of(
            2L, new ProductoCotizado(2L, "Vaso", BigDecimal.TEN, BigDecimal.ZERO)));
        var comando = new PrepararVentaCommand(1L, MetodoPago.TARJETA, List.of(new ProductoSolicitado(2L, 1, false)), null);
        assertThatThrownBy(() -> preparacion.preparar(comando, 9L)).isInstanceOf(RecursoVentaNoEncontradoException.class);
    }

    @Test
    void devuelveLaVentaQueOtraPeticionConfirmoMientrasSeEsperabaElCorte() {
        var caja = mock(CajaPort.class);
        var ventas = mock(VentaRepositorioPort.class);
        when(sucursales.obtenerNegocioIdAccesible(1L, 9L)).thenReturn(Optional.of(5L));
        UUID clave = UUID.randomUUID();
        var venta = new Venta(9L, 5L, 1L, MetodoPago.TARJETA,
            List.of(new DetalleVenta(2L, "Vaso", 1, BigDecimal.TEN, BigDecimal.ZERO)), null);
        var confirmada = new VentaConfirmada(40L, "VTA-000040", 3L, 4L, Instant.now(), venta);
        when(ventas.buscarPorOperacion(1L, clave)).thenReturn(Optional.empty()).thenReturn(Optional.of(confirmada));
        when(caja.bloquearCorteAbierto(1L)).thenReturn(Optional.of(new CajaPort.CorteAbierto(3L, 4L)));

        var comando = new PrepararVentaCommand(1L, MetodoPago.TARJETA, List.of(new ProductoSolicitado(2L, 1, false)), null);
        var servicio = new ConfirmarVentaService(preparacion, caja, inventario, ventas);
        assertThat(servicio.confirmar(comando, 9L, clave)).isSameAs(confirmada);
        verify(ventas, never()).guardar(any(), any(), any());
    }

    @Test
    void siOtraVentaSeLlevoLaExistenciaAlDescontarAvisaCuantoQueda() {
        var contrato = mock(InventarioContrato.class);
        when(contrato.descontarParaVenta(1L, Map.of(2L, 3)))
            .thenReturn(Optional.of(new InventarioContrato.FaltanteVenta(2L, 3, 1)));
        assertThatThrownBy(() -> new InventarioAdapter(contrato).descontarParaVenta(1L, Map.of(2L, 3)))
            .isInstanceOfSatisfying(ExistenciaVentaInsuficienteException.class,
                ex -> assertThat(ex.getExistenciaDisponible()).isEqualTo(1));
    }
}
