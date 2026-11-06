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
            new RegistrarPedidoRequest.Cliente("Ana", "1234567890", "Puebla", "Centro", " "),
            List.of(new RegistrarPedidoRequest.Producto(1L, 0)), "LOCAL", null,
            new RegistrarPedidoRequest.PagoInicial(new BigDecimal("-1"), MetodoPago.EFECTIVO, null));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var campos = factory.getValidator().validate(request).stream()
                .map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(campos.contains("cliente.calle"));
            assertTrue(campos.contains("productos[0].cantidad"));
            assertTrue(campos.contains("pagoInicial.monto"));
        }
    }
}
