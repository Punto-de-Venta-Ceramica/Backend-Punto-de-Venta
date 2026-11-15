package com.iaml.puntoventaapi.pedidos.api;

import static org.junit.jupiter.api.Assertions.*;
import com.iaml.puntoventaapi.pedidos.api.registrar.RegistrarPedidoRequest;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido.MetodoPago;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrarPedidoRequestTest {
    @Test
    void validaDatosAnidados() {
        var request = new RegistrarPedidoRequest(1L, null,
            new RegistrarPedidoRequest.Cliente("Ana", "1234567890", null, " ", null),
            List.of(new RegistrarPedidoRequest.Producto(1L, 0)), "LOCAL", null,
            new RegistrarPedidoRequest.PagoInicial(new BigDecimal("-1"), MetodoPago.EFECTIVO));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var campos = factory.getValidator().validate(request).stream()
                .map(v -> v.getPropertyPath().toString()).toList();
            assertFalse(campos.contains("cliente.ciudad"));
            assertFalse(campos.contains("cliente.colonia"));
            assertFalse(campos.contains("cliente.calle"));
            assertTrue(campos.contains("productos[0].cantidad"));
            assertTrue(campos.contains("pagoInicial.monto"));
        }
    }

    @Test
    void normalizaEspaciosYRechazaNombreOTelefonoInvalidos() {
        var cliente = new RegistrarPedidoRequest.Cliente("  María   José  ", "5512345678",
            "  Ciudad   de México  ", "  5  de Mayo ", " Calle  12 #34 ");
        var request = new RegistrarPedidoRequest(1L, "  Pedido   especial ", cliente,
            List.of(new RegistrarPedidoRequest.Producto(1L, 1)), "LOCAL", "  Entregar   mañana ",
            new RegistrarPedidoRequest.PagoInicial(BigDecimal.ZERO, MetodoPago.EFECTIVO));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
            assertEquals("María José", cliente.nombre());
            assertEquals("Ciudad de México", cliente.ciudad());
            assertEquals("5 de Mayo", cliente.colonia());
            assertEquals("Calle 12 #34", cliente.calle());
            assertEquals("Pedido especial", request.descripcion());
            assertEquals("Entregar mañana", request.descripcionEnvio());
            var invalido = new RegistrarPedidoRequest.Cliente("Ana123", "55 123", null, null, null);
            var campos = factory.getValidator().validate(invalido).stream()
                .map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(campos.contains("nombre"));
            assertTrue(campos.contains("telefono"));
            var conControl = new RegistrarPedidoRequest.Cliente("Ana", "5512345678", "Pue" + (char) 0 + "bla", null, null);
            assertTrue(factory.getValidator().validate(conControl).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("ciudad")));
        }
    }
}
