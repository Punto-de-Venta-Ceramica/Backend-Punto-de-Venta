package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.application.EditarNegocioCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EditarNegocioRequest {

    @NotBlank
    @Size(max = 150)
    private String nombre;

    private MultipartFile logo;

    private boolean eliminarLogo;

    public EditarNegocioCommand toCommand() {
        return new EditarNegocioCommand(nombre, LogoRequestMapper.toDomain(logo), eliminarLogo);
    }
}
