package com.iaml.puntoventaapi.sucursal.application;

public record CrearSucursalCommand(Long negocioId, String nombre, String descripcion,
                                   String ciudad, String calle, String codigoPostal) {
}

