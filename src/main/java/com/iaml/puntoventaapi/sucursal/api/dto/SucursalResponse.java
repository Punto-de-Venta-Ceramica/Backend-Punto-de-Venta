package com.iaml.puntoventaapi.sucursal.api.dto;

import com.iaml.puntoventaapi.sucursal.domain.Sucursal;

public record SucursalResponse(Long id, Long negocioId, String nombre, String descripcion,
                               String ciudad, String calle, String codigoPostal) {

    public static SucursalResponse from(Sucursal sucursal) {
        return new SucursalResponse(sucursal.getId(), sucursal.getNegocioId(), sucursal.getNombre(),
            sucursal.getDescripcion(), sucursal.getCiudad(), sucursal.getCalle(), sucursal.getCodigoPostal());
    }
}

