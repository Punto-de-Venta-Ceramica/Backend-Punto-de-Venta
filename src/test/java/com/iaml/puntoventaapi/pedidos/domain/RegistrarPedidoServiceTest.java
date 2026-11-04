package com.iaml.puntoventaapi.pedidos.domain;

import static org.junit.jupiter.api.Assertions.*;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoCommand;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoService;
import com.iaml.puntoventaapi.pedidos.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.pedidos.domain.port.NotificacionPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RegistrarPedidoServiceTest {
    private abstract static class RepoRegistro implements PedidoRepositorioPort {
        public boolean esPropietarioActual(long pedidoId, long usuarioId) { throw new UnsupportedOperationException(); }
        public Pedido obtenerParaActualizar(long pedidoId, long usuarioId) { throw new UnsupportedOperationException(); }
        public void guardarPago(long pedidoId, PagoPedido pago, long corteId) { throw new UnsupportedOperationException(); }
        public void guardarEstado(long pedidoId, EstadoPedido estado) { throw new UnsupportedOperationException(); }
    }
    @Test
    void usaCatalogoYNotificaDespuesDeGuardar() {
        var escrituras = new AtomicInteger();
        var repo = new RepoRegistro() {
            public void comprobarAcceso(long sucursal, long usuario) { assertEquals(1, usuario); }
            public long guardar(DatosRegistro datos, long cliente, Pedido pedido) {
                assertEquals(new BigDecimal("180.00"), pedido.total());
                escrituras.incrementAndGet();
                return 7;
            }
        };
        CatalogoPort catalogo = (s, u, p) -> new CatalogoPort.Producto(new BigDecimal("100.00"), 2, null, new BigDecimal("10.00"));
        var service = new RegistrarPedidoService(repo, catalogo, cliente -> 5, new NotificacionPort() {
            public void pedidoRegistrado(long pedido, long sucursal, long usuario) {
                assertEquals(1, escrituras.get());
                assertEquals(7, pedido);
                escrituras.incrementAndGet();
            }
            public void pedidoEntregado(long pedidoId) { throw new AssertionError(); }
        });
        var cliente = new ClientePedido("Ana", "123", "Puebla", "Centro", "Calle 1");
        var comando = new RegistrarPedidoCommand(2, 1, null, cliente,
            List.of(new RegistrarPedidoCommand.ProductoSolicitado(3, 2)), "LOCAL", null,
            new BigDecimal("50.00"), PagoPedido.MetodoPago.EFECTIVO, 1L);
        var resultado = service.registrar(comando);
        assertEquals(new BigDecimal("130.00"), resultado.saldoPendiente());
        assertEquals(2, escrituras.get());
    }

    @Test
    void rechazaSobrepagoAntesDeGuardarClienteOPedido() {
        var escrituras = new AtomicInteger();
        var repo = new RepoRegistro() {
            public void comprobarAcceso(long sucursal, long usuario) { }
            public long guardar(DatosRegistro datos, long cliente, Pedido pedido) { escrituras.incrementAndGet(); return 1; }
        };
        var service = new RegistrarPedidoService(repo,
            (s, u, p) -> new CatalogoPort.Producto(new BigDecimal("100.00"), null, null, null),
            cliente -> { escrituras.incrementAndGet(); return 1; }, new NotificacionPort() {
                public void pedidoRegistrado(long p, long s, long u) { escrituras.incrementAndGet(); }
                public void pedidoEntregado(long p) { escrituras.incrementAndGet(); }
            });
        var cliente = new ClientePedido("Ana", "123", "Puebla", "Centro", "Calle 1");
        var comando = new RegistrarPedidoCommand(2, 1, null, cliente,
            List.of(new RegistrarPedidoCommand.ProductoSolicitado(3, 1)), "LOCAL", null,
            new BigDecimal("101.00"), PagoPedido.MetodoPago.TARJETA, 1L);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(comando));
        assertEquals(0, escrituras.get());
    }
}
