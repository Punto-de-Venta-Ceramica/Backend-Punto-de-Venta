package com.iaml.puntoventaapi.pedidos.api.editar;

import com.iaml.puntoventaapi.pedidos.api.registrar.RegistrarPedidoRequest;
import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.TextoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mismas reglas de texto que el registro del pedido. */
public record EditarDatosPedidoRequest(
        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres.")
        @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La descripción contiene caracteres no válidos.") String descripcion,
        @NotBlank(message = "El tipo de entrega es obligatorio.")
        @Size(max = 50, message = "El tipo de entrega no debe superar 50 caracteres.") String tipoEntrega,
        @Size(max = 2000, message = "La descripción de envío no debe superar 2000 caracteres.")
        @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La descripción de envío contiene caracteres no válidos.") String descripcionEnvio,
        @NotNull(message = "Los datos del cliente son obligatorios.") @Valid RegistrarPedidoRequest.Cliente cliente) {
    public EditarDatosPedidoRequest {
        descripcion = TextoPedido.normalizar(descripcion);
        descripcionEnvio = TextoPedido.normalizar(descripcionEnvio);
    }

    public ClientePedido clienteDomain() {
        return new ClientePedido(cliente.nombre(), cliente.telefono(), cliente.ciudad(), cliente.colonia(), cliente.calle());
    }
    public PedidoRepositorioPort.DatosEdicion datosDomain() {
        return new PedidoRepositorioPort.DatosEdicion(descripcion, tipoEntrega, descripcionEnvio);
    }
}
