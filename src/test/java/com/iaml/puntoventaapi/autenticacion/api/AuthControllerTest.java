package com.iaml.puntoventaapi.autenticacion.api;

import com.iaml.puntoventaapi.autenticacion.api.dto.LoginRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.LoginResponse;
import com.iaml.puntoventaapi.autenticacion.api.dto.RefreshRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.RestablecerPasswordRequest;
import com.iaml.puntoventaapi.autenticacion.api.dto.SolicitarCodigoRequest;
import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionCommand;
import com.iaml.puntoventaapi.autenticacion.application.IniciarSesionService;
import com.iaml.puntoventaapi.autenticacion.application.RenovarSesionService;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private static final Sesion SESION =
        new Sesion("acceso", Instant.EPOCH, "renovacion", Instant.EPOCH, 7L, "PROPIETARIO");

    private IniciarSesionService iniciar;
    private RenovarSesionService renovar;
    private AuthController controller;

    @BeforeEach
    void preparar() {
        iniciar = mock(IniciarSesionService.class);
        renovar = mock(RenovarSesionService.class);
        controller = new AuthController(iniciar, renovar);
    }

    @Test
    void loginDevuelveLaSesionComoBearer() {
        when(iniciar.iniciarSesion(new IniciarSesionCommand("a@b.mx", "x"))).thenReturn(SESION);

        LoginResponse respuesta = controller.login(new LoginRequest("a@b.mx", "x"));

        assertEquals("Bearer", respuesta.tipo());
        assertEquals("acceso", respuesta.token());
        assertEquals("renovacion", respuesta.refreshToken());
        assertEquals(7L, respuesta.usuarioId());
        assertEquals("PROPIETARIO", respuesta.rol());
    }

    @Test
    void refreshRenuevaLaSesion() {
        when(renovar.renovar("token")).thenReturn(SESION);

        assertEquals("acceso", controller.refresh(new RefreshRequest("token")).token());
    }

    @Test
    void traduceLasExcepcionesAEstadosHttp() {
        ProblemDetail demasiados = controller.demasiadosIntentos(new DemasiadosIntentosException());
        ProblemDetail invalidas = controller.credencialesInvalidas(new CredencialesInvalidasException());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), demasiados.getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), invalidas.getStatus());
    }

    @Test
    void dtosPendientesSePuedenInstanciar() {
        assertNotNull(new RestablecerPasswordRequest());
        assertNotNull(new SolicitarCodigoRequest());
    }
}
