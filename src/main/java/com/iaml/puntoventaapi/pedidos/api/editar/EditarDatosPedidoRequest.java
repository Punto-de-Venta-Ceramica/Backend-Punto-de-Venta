package com.iaml.puntoventaapi.pedidos.api.editar;

import com.iaml.puntoventaapi.pedidos.api.registrar.RegistrarPedidoRequest;
import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EditarDatosPedidoRequest(
        @Size(max = 2000) String descripcion,
        @NotBlank(message = "El tipo de entrega es obligatorio.") @Size(max = 50) String tipoEntrega,
        @Size(max = 2000) String descripcionEnvio,
        @NotNull(message = "Los datos del cliente son obligatorios.") @Valid RegistrarPedidoRequest.Cliente cliente) {
    public ClientePedido clienteDomain() {
        return new ClientePedido(cliente.nombre(), cliente.telefono(), cliente.ciudad(), cliente.colonia(), cliente.calle());
    }
    public PedidoRepositorioPort.DatosEdicion datosDomain() {
        return new PedidoRepositorioPort.DatosEdicion(descripcion, tipoEntrega, descripcionEnvio);
    }
}
