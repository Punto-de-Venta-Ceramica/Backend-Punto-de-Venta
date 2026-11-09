package com.iaml.puntoventaapi.pedidos.api.abonar;

import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RegistrarPagoRequest(
        @NotNull(message = "El monto es obligatorio.")
        @DecimalMin(value = "0.00", inclusive = false, message = "El monto debe ser mayor a cero.")
        @Digits(integer = 16, fraction = 2, message = "El monto admite hasta dos decimales.") BigDecimal monto,
        @NotNull(message = "El método de pago es obligatorio.") PagoPedido.MetodoPago metodoPago) {
    public PagoPedido toDomain() { return new PagoPedido(monto, metodoPago); }
}
