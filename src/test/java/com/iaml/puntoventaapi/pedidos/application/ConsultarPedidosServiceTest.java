package com.iaml.puntoventaapi.pedidos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.iaml.puntoventaapi.pedidos.application.consultar.ConsultarPedidosService;
import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultarPedidosServiceTest {

    private static final class Consultas implements ConsultaPedidosPort {
        EstadoPedido estado;
        int pagina = -1;
        int tamanio = -1;

        @Override
        public Vista consultar(long pedidoId, long usuarioId) { throw new UnsupportedOperationException(); }

        @Override
        public Pagina<Resumen> listar(long sucursalId, long usuarioId, EstadoPedido estado, int pagina, int tamanio) {
            this.estado = estado;
            this.pagina = pagina;
            this.tamanio = tamanio;
            return new Pagina<>(List.of(), pagina, tamanio, 0);
        }
    }

    @Test
    void pideVeintePorPaginaConElEstadoIndicado() {
        var consultas = new Consultas();
        var pagina = new ConsultarPedidosService(consultas).listar(1, 2, EstadoPedido.ENCARGADO, 3);
        assertEquals(EstadoPedido.ENCARGADO, consultas.estado);
        assertEquals(3, consultas.pagina);
        assertEquals(20, consultas.tamanio);
        assertTrue(pagina.contenido().isEmpty());
    }

    @Test
    void sinEstadoPideTodos() {
        var consultas = new Consultas();
        new ConsultarPedidosService(consultas).listar(1, 2, null, 0);
        assertEquals(null, consultas.estado);
        assertEquals(0, consultas.pagina);
    }

    @Test
    void rechazaPaginaNegativaSinConsultar() {
        var consultas = new Consultas();
        assertThrows(IllegalArgumentException.class,
            () -> new ConsultarPedidosService(consultas).listar(1, 2, null, -1));
        assertEquals(-1, consultas.pagina);
    }
}
