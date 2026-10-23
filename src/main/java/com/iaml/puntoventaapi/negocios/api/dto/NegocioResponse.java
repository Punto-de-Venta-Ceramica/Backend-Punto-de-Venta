package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NegocioResponse {

    private Long id;
    private String nombre;
    private String logo;

    public static NegocioResponse from(Negocio negocio) {
        return new NegocioResponse(negocio.getId(), negocio.getNombre(), negocio.getLogo());
    }
}
