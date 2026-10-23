package com.iaml.puntoventaapi.usuarios.api;

import com.iaml.puntoventaapi.usuarios.api.dto.RegistrarPropietarioRequest;
import com.iaml.puntoventaapi.usuarios.api.dto.UsuarioResponse;
import com.iaml.puntoventaapi.usuarios.application.PropietarioService;
import com.iaml.puntoventaapi.usuarios.domain.exception.CorreoYaRegistradoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PropietarioService propietarioService;

    public UsuarioController(PropietarioService propietarioService) {
        this.propietarioService = propietarioService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrarPropietario(@Valid @RequestBody RegistrarPropietarioRequest request) {
        return UsuarioResponse.from(propietarioService.registrar(request.toCommand()));
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ProblemDetail correoYaRegistrado(CorreoYaRegistradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
