package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.application.CrearNegocioCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CrearNegocioRequest {

    @NotBlank
    @Size(max = 150)
    private String nombre;

    private MultipartFile logo;

    public CrearNegocioCommand toCommand() {
        return new CrearNegocioCommand(nombre, LogoRequestMapper.toDomain(logo));
    }
}
