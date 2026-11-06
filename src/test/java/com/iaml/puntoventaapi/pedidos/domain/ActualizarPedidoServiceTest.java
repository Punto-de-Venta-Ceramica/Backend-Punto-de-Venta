package com.iaml.puntoventaapi.pedidos.domain;

import static org.junit.jupiter.api.Assertions.*;
import com.iaml.puntoventaapi.pedidos.application.abonar.RegistrarPagoService;
import com.iaml.puntoventaapi.pedidos.application.cambiarestado.CambiarEstadoService;
import com.iaml.puntoventaapi.pedidos.domain.port.NotificacionPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ActualizarPedidoServiceTest {
    private static class Repositorio implements PedidoRepositorioPort {
        Pedido pedido = Pedido.restaurar(
            List.of(new DetallePedido(1, 1, new BigDecimal("100.00"), BigDecimal.ZERO)),
            List.of(new PagoPedido(new BigDecimal("30.00"), PagoPedido.MetodoPago.EFECTIVO)), EstadoPedido.ENCARGADO);
        int pagosGuardados;
        int estadosGuardados;
        boolean propietario;
        public void comprobarAcceso(long sucursalId, long usuarioId) { throw new UnsupportedOperationException(); }
        public long guardar(DatosRegistro datos, long clienteId, Pedido pedido) { throw new UnsupportedOperationException(); }
        public Pedido obtenerParaActualizar(long pedidoId, long usuarioId) { return pedido; }
        public boolean esPropietarioActual(long pedidoId, long usuarioId) { return propietario; }
        public void guardarPago(long pedidoId, PagoPedido pago, long corteId) { pagosGuardados++; }
        public void guardarEstado(long pedidoId, EstadoPedido estado) { estadosGuardados++; }
    }

    @Test
    void registraUnAbonoSinPermitirExcederElSaldo() {
        var repo = new Repositorio();
        var service = new RegistrarPagoService(repo);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, 2,
            new PagoPedido(new BigDecimal("71.00"), PagoPedido.MetodoPago.TARJETA), 1));
        assertEquals(0, repo.pagosGuardados);
        var resultado = service.registrar(1, 2, new PagoPedido(new BigDecimal("70.00"), PagoPedido.MetodoPago.TARJETA), 1);
        assertTrue(resultado.pagado());
        assertEquals(new BigDecimal("0.00"), resultado.saldoPendiente());
        assertEquals(1, repo.pagosGuardados);
    }

    @Test
    void avisaLaEntregaUnaSolaVezYRestringeProduccion() {
        var repo = new Repositorio();
        var avisos = new AtomicInteger();
        var service = new CambiarEstadoService(repo, new NotificacionPort() {
            public void pedidoRegistrado(long p, long s, long u) { throw new AssertionError(); }
            public void pedidoEntregado(long p) { avisos.incrementAndGet(); }
        });
        assertThrows(SecurityException.class, () -> service.cambiar(1, 2, EstadoPedido.PRODUCCION));
        assertEquals(0, repo.estadosGuardados);
        repo.propietario = true;
        service.cambiar(1, 2, EstadoPedido.PRODUCCION);
        repo.propietario = false;
        service.cambiar(1, 2, EstadoPedido.ENTREGADO);
        service.cambiar(1, 2, EstadoPedido.ENTREGADO);
        assertEquals(1, avisos.get());
    }
}
