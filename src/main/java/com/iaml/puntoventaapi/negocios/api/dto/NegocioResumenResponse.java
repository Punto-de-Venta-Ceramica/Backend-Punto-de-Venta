package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NegocioResumenResponse {

    private Long id;
    private String nombre;

    public static NegocioResumenResponse from(NegocioResumen negocio) {
        return new NegocioResumenResponse(negocio.id(), negocio.nombre());
    }
}
