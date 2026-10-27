package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class NegocioResponse {

    private Long id;
    private String nombre;
    private String logo;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
    private Instant fechaEliminacion;
    private boolean activo;

    public static NegocioResponse from(Negocio negocio) {
        return new NegocioResponse(negocio.getId(), negocio.getNombre(), negocio.getLogo(),
            negocio.getFechaCreacion(), negocio.getFechaActualizacion(), negocio.getFechaEliminacion(),
            negocio.isActivo());
    }
}
