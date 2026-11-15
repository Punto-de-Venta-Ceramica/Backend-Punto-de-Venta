package com.iaml.puntoventaapi.pedidos.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.iaml.puntoventaapi.pedidos.domain.exception.TransicionEstadoInvalidaException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El 409 de un cambio de estado inválido dice en qué estado está el pedido y a cuál puede pasar. */
class TransicionEstadoTest {

    private static Pedido pedido(EstadoPedido estado) {
        var detalle = new DetallePedido(1, 1, new BigDecimal("10.00"), BigDecimal.ZERO);
        return Pedido.restaurar(List.of(detalle), List.of(), estado);
    }

    @Test
    void saltarDeEncargadoAEntregadoDiceElEstadoActual() {
        var ex = assertThrows(TransicionEstadoInvalidaException.class,
            () -> pedido(EstadoPedido.ENCARGADO).cambiarEstado(EstadoPedido.ENTREGADO, true));
        assertEquals(EstadoPedido.ENCARGADO, ex.getEstadoActual());
        assertEquals("El pedido está en Encargado; solo puede pasar a En producción o cancelarse.", ex.getMessage());
    }

    @Test
    void unPedidoTerminadoOCanceladoYaNoCambia() {
        assertEquals("El pedido ya está terminado y no puede cambiar de estado.",
            assertThrows(TransicionEstadoInvalidaException.class,
                () -> pedido(EstadoPedido.TERMINADO).cambiarEstado(EstadoPedido.ENTREGADO, true)).getMessage());
        assertEquals("El pedido está cancelado y ya no puede cambiar de estado.",
            assertThrows(TransicionEstadoInvalidaException.class,
                () -> pedido(EstadoPedido.CANCELADO).cambiarEstado(EstadoPedido.TERMINADO, true)).getMessage());
    }
}
