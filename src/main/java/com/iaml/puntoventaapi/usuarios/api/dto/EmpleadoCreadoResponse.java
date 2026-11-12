package com.iaml.puntoventaapi.usuarios.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iaml.puntoventaapi.usuarios.application.EmpleadoCreado;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmpleadoCreadoResponse(
        Long id,
        Long negocioId,
        String nombre,
        String apellidoP,
        String apellidoM,
        String correoElectronico,
        Long sucursalId,
        String sucursalNombre,
        String passwordGenerada) {

    public static EmpleadoCreadoResponse from(EmpleadoCreado creado) {
        Usuario u = creado.usuario();
        return new EmpleadoCreadoResponse(
                u.getId(),
                u.getNegocioId(),
                u.getNombre(),
                u.getApellidoPaterno(),
                u.getApellidoMaterno(),
                u.getCorreo(),
                u.getSucursalIds().isEmpty() ? null : u.getSucursalIds().get(0),
                creado.sucursalNombre(),
                creado.passwordGenerada()
        );
    }
}

