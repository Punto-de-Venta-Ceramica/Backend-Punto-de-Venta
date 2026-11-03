package com.iaml.puntoventaapi.catalogo.api.dto;

import com.iaml.puntoventaapi.catalogo.domain.Area;

public record AreaResponse(Long id, Long negocioId, String nombre, String descripcion) {

    public static AreaResponse from(Area area) {
        return new AreaResponse(area.getId(), area.getNegocioId(), area.getNombre(), area.getDescripcion());
    }
}
