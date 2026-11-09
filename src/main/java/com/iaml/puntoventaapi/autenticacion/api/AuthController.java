package com.iaml.puntoventaapi.autenticacion.api;

import com.iaml.puntoventaapi.autenticacion.api.dto.LoginRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.LoginResponse;
import com.iaml.puntoventaapi.autenticacion.api.dto.RefreshRequest;
import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionService;
import com.iaml.puntoventaapi.autenticacion.application.RenovarSesionService;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final IniciarSesionService iniciarSesionService;

    private final RenovarSesionService renovarSesionService;

    public AuthController(IniciarSesionService iniciarSesionService, RenovarSesionService renovarSesionService) {
        this.iniciarSesionService = iniciarSesionService;
        this.renovarSesionService = renovarSesionService;
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return LoginResponse.from(renovarSesionService.renovar(request.refreshToken()));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(iniciarSesionService.iniciarSesion(request.toCommand()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
