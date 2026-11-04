package com.iaml.puntoventaapi.pedidos.domain;

import static org.junit.jupiter.api.Assertions.*;
import com.iaml.puntoventaapi.pedidos.application.editar.EditarDatosPedidoService;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class EditarDatosPedidoServiceTest {
    private static class Repositorio implements PedidoRepositorioPort {
        boolean propietario;
        int actualizaciones;
        EstadoPedido estado = EstadoPedido.ENCARGADO;
        public void comprobarAcceso(long sucursal, long usuario) { }
        public long guardar(DatosRegistro datos, long cliente, Pedido pedido) { throw new UnsupportedOperationException(); }
        public Pedido obtenerParaActualizar(long pedido, long usuario) {
            return Pedido.restaurar(List.of(new DetallePedido(1, 1, new BigDecimal("50.00"), BigDecimal.ZERO)),
                List.of(), estado);
        }
        public boolean esPropietarioActual(long pedido, long usuario) { return propietario; }
        public void actualizarDatos(long pedido, long cliente, DatosEdicion datos) { actualizaciones++; }
        public void guardarPago(long pedido, PagoPedido pago, long corte) { throw new UnsupportedOperationException(); }
        public void guardarEstado(long pedido, EstadoPedido estado) { throw new UnsupportedOperationException(); }
    }

    @Test
    void soloPropietarioPuedeModificarDatosSinTocarOtrosPedidos() {
        var repo = new Repositorio();
        var clientes = new AtomicInteger();
        var servicio = new EditarDatosPedidoService(repo, datos -> { clientes.incrementAndGet(); return 4; });
        var cliente = new ClientePedido("Ana", "123", "Puebla", "Centro", "Calle 1");
        var datos = new PedidoRepositorioPort.DatosEdicion("Nueva descripción", "LOCAL", null);
        assertThrows(AccessDeniedException.class, () -> servicio.editar(1, 2, cliente, datos));
        assertEquals(0, clientes.get());
        assertEquals(0, repo.actualizaciones);
        repo.propietario = true;
        servicio.editar(1, 2, cliente, datos);
        assertEquals(1, clientes.get());
        assertEquals(1, repo.actualizaciones);
        repo.estado = EstadoPedido.CANCELADO;
        assertThrows(IllegalStateException.class, () -> servicio.editar(1, 2, cliente, datos));
        assertEquals(1, clientes.get());
    }
}
