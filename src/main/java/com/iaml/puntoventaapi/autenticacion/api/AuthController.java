package com.iaml.puntoventaapi.autenticacion.api;

import com.iaml.puntoventaapi.autenticacion.api.dto.CambiarPasswordRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.LoginRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.LoginResponse;
import com.iaml.puntoventaapi.autenticacion.api.dto.RefreshRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.RestablecerPasswordRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.SolicitarCodigoRequest;
import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionService;
import com.iaml.puntoventaapi.autenticacion.application.RecuperarPasswordService;
import com.iaml.puntoventaapi.autenticacion.application.RenovarSesionService;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.exception.DemasiadosIntentosException;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticacion", description = "Login, refresco de sesion, cambio y recuperacion de contrasena")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IniciarSesionService iniciarSesionService;
    private final RenovarSesionService renovarSesionService;
    private final RecuperarPasswordService recuperarPasswordService;
    private final UsuarioActual usuarioActual;

    public AuthController(
            IniciarSesionService iniciarSesionService,
            RenovarSesionService renovarSesionService,
            RecuperarPasswordService recuperarPasswordService,
            UsuarioActual usuarioActual) {
        this.iniciarSesionService = iniciarSesionService;
        this.renovarSesionService = renovarSesionService;
        this.recuperarPasswordService = recuperarPasswordService;
        this.usuarioActual = usuarioActual;
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica un usuario y devuelve accessToken y refreshToken.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sesion iniciada correctamente"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(iniciarSesionService.iniciarSesion(request.toCommand()));
    }

    @Operation(summary = "Renovar token", description = "Genera un nuevo accessToken usando el refreshToken.")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return LoginResponse.from(renovarSesionService.renovar(request.refreshToken()));
    }

    @Operation(summary = "Cambiar contrasena", description = "Permite al usuario autenticado cambiar su contrasena actual.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Contrasena cambiada correctamente"),
        @ApiResponse(responseCode = "400", description = "Las contrasenas no coinciden o datos invalidos"),
        @ApiResponse(responseCode = "401", description = "Contrasena actual incorrecta")
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req) {
        recuperarPasswordService.cambiarPassword(
                usuarioActual.id(),
                req.passwordActual(),
                req.nuevaPassword(),
                req.confirmarPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Solicitar codigo de recuperacion",
            description = "Envia un codigo de 6 digitos al correo del usuario. El codigo es valido 10 minutos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Codigo enviado al correo"),
        @ApiResponse(responseCode = "400", description = "Correo no registrado o invalido")
    })
    @PostMapping("/forgot")
    public ResponseEntity<java.util.Map<String, String>> solicitarCodigo(@Valid @RequestBody SolicitarCodigoRequest req) {
        recuperarPasswordService.solicitarCodigo(req.correo());
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Codigo de verificacion enviado al correo."));
    }

    @Operation(
        summary = "Restablecer contrasena con codigo",
        description = "Verifica el codigo recibido por correo y establece la nueva contrasena."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Contrasena restablecida correctamente"),
        @ApiResponse(responseCode = "400", description = "Codigo incorrecto, expirado, o contrasenas no coinciden")
    })
    @PatchMapping("/forgot/password")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest req) {
        recuperarPasswordService.restablecerPassword(
            req.correo(),
            req.codigo(),
            req.nuevaPassword(),
            req.confirmarPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ProblemDetail demasiadosIntentos(DemasiadosIntentosException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
