package com.iaml.puntoventaapi.pedidos.api.cambiarestado;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(@NotNull(message = "El estado es obligatorio.") EstadoPedido estado) { }
