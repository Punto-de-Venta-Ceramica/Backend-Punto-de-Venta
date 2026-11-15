package com.iaml.puntoventaapi.negocios.api.dto;

import com.iaml.puntoventaapi.negocios.application.EditarNegocioCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EditarNegocioRequest {

    @NotBlank
    @Size(max = 150)
    private String nombre;

    private MultipartFile logo;

    @Size(max = 150, message = "El eslogan no debe superar 150 caracteres.")
    @Pattern(regexp = "^[\\P{Cc}]*$", message = "El eslogan contiene caracteres no permitidos.")
    private String eslogan;

    private boolean eliminarLogo;

    public EditarNegocioCommand toCommand() {
        return new EditarNegocioCommand(nombre, LogoRequestMapper.toDomain(logo), eliminarLogo, eslogan);
    }
}
