package com.iaml.puntoventaapi.pedidos.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PedidoTest {
    private static BigDecimal cantidad(String valor) { return new BigDecimal(valor); }

    @Test
    void calculaPrecioConDescuentoYAdmitePagosHastaLiquidar() {
        var pedido = new Pedido(List.of(new DetallePedido(1, 3, cantidad("100.00"), cantidad("10.00"))));
        assertEquals(cantidad("270.00"), pedido.total());
        pedido.registrarPago(new PagoPedido(cantidad("100.00"), PagoPedido.MetodoPago.EFECTIVO));
        assertEquals(cantidad("170.00"), pedido.saldoPendiente());
        assertFalse(pedido.pagado());
        assertThrows(PagoExcedeSaldoException.class,
            () -> pedido.registrarPago(new PagoPedido(cantidad("171.00"), PagoPedido.MetodoPago.TARJETA)));
        pedido.registrarPago(new PagoPedido(cantidad("170.00"), PagoPedido.MetodoPago.TARJETA));
        assertTrue(pedido.pagado());
    }

    @Test
    void reservaProduccionYCancelacionAlPropietario() {
        var pedido = new Pedido(List.of(new DetallePedido(1, 1, cantidad("50.00"), cantidad("0.00"))));
        assertThrows(SecurityException.class, () -> pedido.cambiarEstado(EstadoPedido.PRODUCCION, false));
        assertThrows(SecurityException.class, () -> pedido.cambiarEstado(EstadoPedido.CANCELADO, false));
        pedido.cambiarEstado(EstadoPedido.PRODUCCION, true);
        pedido.cambiarEstado(EstadoPedido.ENTREGADO, false);
        assertEquals(EstadoPedido.ENTREGADO, pedido.estado());
        pedido.cambiarEstado(EstadoPedido.CANCELADO, true);
        assertThrows(IllegalStateException.class, () -> pedido.cambiarEstado(EstadoPedido.ENCARGADO, true));
        assertThrows(IllegalArgumentException.class,
            () -> pedido.registrarPago(new PagoPedido(cantidad("1.00"), PagoPedido.MetodoPago.EFECTIVO)));
    }

    @Test
    void impideSaltarORetrocederEstados() {
        var pedido = new Pedido(List.of(new DetallePedido(1, 1, cantidad("50.00"), cantidad("0.00"))));
        assertThrows(IllegalStateException.class, () -> pedido.cambiarEstado(EstadoPedido.ENTREGADO, false));
        pedido.cambiarEstado(EstadoPedido.PRODUCCION, true);
        assertThrows(IllegalStateException.class, () -> pedido.cambiarEstado(EstadoPedido.ENCARGADO, true));
        assertThrows(IllegalStateException.class, () -> pedido.cambiarEstado(EstadoPedido.TERMINADO, true));
        pedido.cambiarEstado(EstadoPedido.ENTREGADO, false);
        pedido.cambiarEstado(EstadoPedido.TERMINADO, false);
        assertEquals(EstadoPedido.TERMINADO, pedido.estado());
    }

    @Test
    void exigeDatosDelClienteYProductosSinDuplicados() {
        assertThrows(IllegalArgumentException.class, () -> new ClientePedido("Ana123", "5512345678", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new ClientePedido("Ana", "123", null, null, null));
        var cliente = new ClientePedido("  María   José  ", "5512345678", "  ", "  5   de Mayo ", " Calle  12 #34 ");
        assertEquals("María José", cliente.nombre());
        assertNull(cliente.ciudad());
        assertEquals("5 de Mayo", cliente.colonia());
        assertEquals("Calle 12 #34", cliente.calle());
        var detalle = new DetallePedido(1, 1, cantidad("10.00"), cantidad("0.00"));
        assertThrows(IllegalArgumentException.class, () -> new Pedido(List.of(detalle, detalle)));
        assertThrows(IllegalArgumentException.class, () -> new Pedido(List.of()));
    }

    @Test
    void aplicaDescuentoSoloCuandoLaCantidadEstaEnElRango() {
        var sinDescuento = DetallePedido.desdeCatalogo(1, 4, cantidad("25.00"), 5, 10, cantidad("15.00"));
        var conDescuento = DetallePedido.desdeCatalogo(1, 5, cantidad("25.00"), 5, 10, cantidad("15.00"));
        assertEquals(cantidad("100.00"), sinDescuento.subtotal());
        assertEquals(cantidad("106.25"), conDescuento.subtotal());
    }
}
