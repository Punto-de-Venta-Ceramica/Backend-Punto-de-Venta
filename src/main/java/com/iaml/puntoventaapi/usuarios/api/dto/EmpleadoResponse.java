package com.iaml.puntoventaapi.usuarios.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iaml.puntoventaapi.usuarios.application.EmpleadoDetalle;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmpleadoResponse(
        Long id,
        Long negocioId,
        String nombre,
        String apellidoP,
        String apellidoM,
        String correoElectronico,
        Long sucursalId,
        String sucursalNombre,
        java.util.List<SucursalEmpleadoResponse> sucursales,
        Boolean asignado) {

    public static EmpleadoResponse from(EmpleadoDetalle detalle) {
        Usuario u = detalle.usuario();
        return new EmpleadoResponse(
                u.getId(),
                u.getNegocioId(),
                u.getNombre(),
                u.getApellidoPaterno(),
                u.getApellidoMaterno(),
                u.getCorreo(),
                detalle.sucursales().isEmpty() ? null : detalle.sucursales().get(0).id(),
                detalle.sucursales().isEmpty() ? null : detalle.sucursales().get(0).nombre(),
                detalle.asignado() == null ? null
                        : detalle.sucursales().stream()
                                .map(s -> new SucursalEmpleadoResponse(s.id(), s.nombre())).toList(),
                detalle.asignado()
        );
    }
}

