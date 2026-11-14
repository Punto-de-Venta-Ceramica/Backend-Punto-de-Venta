package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.CodigoRecuperacion;
import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Rol;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.exception.DemasiadosIntentosException;
import com.iaml.puntoventaapi.autenticacion.domain.port.CredencialRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.GeneradorHashPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.PasswordHasherPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.RefreshTokenPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.RefreshTokenPort.RefreshValido;
import com.iaml.puntoventaapi.autenticacion.domain.port.RolRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.TokenPort;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacionServiciosTest {

    private static final Credencial CREDENCIAL =
        new Credencial(7L, "dueno@barro.mx", "hash", new Rol(1L, Rol.PROPIETARIO, Set.of()));
    private static final Sesion SESION = new Sesion("a", Instant.EPOCH, "r", Instant.EPOCH, 7L, "PROPIETARIO");

    // ---- RenovarSesionService ----

    @Test
    void renovarEmiteNuevaSesionConElTokenVigente() {
        CredencialRepositorioPort credenciales = mock(CredencialRepositorioPort.class);
        RefreshTokenPort refresh = mock(RefreshTokenPort.class);
        TokenPort tokens = mock(TokenPort.class);
        when(refresh.validar("r")).thenReturn(new RefreshValido(7L, Instant.EPOCH));
        when(credenciales.buscarPorUsuarioId(7L)).thenReturn(Optional.of(CREDENCIAL));
        when(tokens.generar(CREDENCIAL)).thenReturn(SESION);

        assertSame(SESION, new RenovarSesionService(credenciales, refresh, tokens).renovar("r"));
    }

    @Test
    void renovarFallaSiElUsuarioYaNoExiste() {
        CredencialRepositorioPort credenciales = mock(CredencialRepositorioPort.class);
        RefreshTokenPort refresh = mock(RefreshTokenPort.class);
        when(refresh.validar("r")).thenReturn(new RefreshValido(7L, Instant.EPOCH));
        when(credenciales.buscarPorUsuarioId(7L)).thenReturn(Optional.empty());

        RenovarSesionService service = new RenovarSesionService(credenciales, refresh, mock(TokenPort.class));
        assertThrows(CredencialesInvalidasException.class, () -> service.renovar("r"));
    }

    @Test
    void renovarFallaSiLaPasswordCambioDespuesDeEmitirElToken() {
        CredencialRepositorioPort credenciales = mock(CredencialRepositorioPort.class);
        RefreshTokenPort refresh = mock(RefreshTokenPort.class);
        when(refresh.validar("r")).thenReturn(new RefreshValido(7L, Instant.EPOCH, "huella-vieja"));
        when(credenciales.buscarPorUsuarioId(7L)).thenReturn(Optional.of(CREDENCIAL));

        RenovarSesionService service = new RenovarSesionService(credenciales, refresh, mock(TokenPort.class));
        assertThrows(CredencialesInvalidasException.class, () -> service.renovar("r"));
    }

    @Test
    void refreshValidoCompruebaLaHuella() {
        assertTrue(new RefreshValido(7L, Instant.EPOCH, CREDENCIAL.huellaPassword()).vigentePara(CREDENCIAL));
        assertTrue(new RefreshValido(7L, Instant.EPOCH).vigentePara(CREDENCIAL));
        assertFalse(new RefreshValido(7L, Instant.EPOCH, "otra").vigentePara(CREDENCIAL));
    }

    // ---- AutenticacionContratoService ----

    @Test
    void contratoDelegaEnLosPuertos() {
        GeneradorHashPort hash = mock(GeneradorHashPort.class);
        RolRepositorioPort roles = mock(RolRepositorioPort.class);
        when(hash.hashear("plano")).thenReturn("hasheado");
        when(roles.buscarIdPorNombre("EMPLEADO")).thenReturn(Optional.of(2L));
        AutenticacionContratoService service = new AutenticacionContratoService(hash, roles);

        assertEquals("hasheado", service.hashearPassword("plano"));
        assertEquals(Optional.of(2L), service.buscarRolIdPorNombre("EMPLEADO"));
    }

    // ---- IniciarSesionService: contraseña nula con correo inexistente ----

    @Test
    void correoInexistenteConPasswordNulaTambienSeRechaza() {
        CredencialRepositorioPort credenciales = mock(CredencialRepositorioPort.class);
        when(credenciales.buscarPorCorreo(anyString())).thenReturn(Optional.empty());
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        IniciarSesionService service = new IniciarSesionService(credenciales, hasher, mock(TokenPort.class));

        assertThrows(CredencialesInvalidasException.class,
            () -> service.iniciarSesion(new IniciarSesionCommand("nadie@barro.mx", null)));
        verify(hasher).coincide(eq(""), anyString());
    }

    // ---- ControlIntentos ----

    @Test
    void elBloqueoExpiraCuandoPasaLaVentana() {
        AtomicReference<Instant> reloj = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        ControlIntentos control = new ControlIntentos(reloj::get);
        for (int i = 0; i < ControlIntentos.MAXIMO; i++) {
            control.registrarFallo("Ana@Barro.mx ");
        }
        assertThrows(DemasiadosIntentosException.class, () -> control.verificar("ana@barro.mx"));

        reloj.set(reloj.get().plus(ControlIntentos.VENTANA).plusSeconds(1));
        assertDoesNotThrow(() -> control.verificar("ana@barro.mx"));

        // Un fallo nuevo después de la ventana reinicia la cuenta en 1.
        control.registrarFallo("ana@barro.mx");
        assertDoesNotThrow(() -> control.verificar("ana@barro.mx"));
    }

    @Test
    void limpiarBorraLaCuentaYElCorreoNuloSeAgrupaEnUnaClaveVacia() {
        ControlIntentos control = new ControlIntentos();
        for (int i = 0; i < ControlIntentos.MAXIMO; i++) {
            control.registrarFallo(null);
        }
        assertThrows(DemasiadosIntentosException.class, () -> control.verificar(""));
        control.limpiar(null);
        assertDoesNotThrow(() -> control.verificar(null));
    }

    @Test
    void sinFallosPreviosNoBloquea() {
        ControlIntentos control = new ControlIntentos();
        control.registrarFallo("a@b.mx");
        assertDoesNotThrow(() -> control.verificar("a@b.mx"));
        assertDoesNotThrow(() -> control.verificar("otro@b.mx"));
    }

    @Test
    void alLlegarAlLimiteDeEntradasSeDescartanLasVencidas() {
        AtomicReference<Instant> reloj = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        ControlIntentos control = new ControlIntentos(reloj::get);
        for (int i = 0; i < 10_000; i++) {
            control.registrarFallo("u" + i + "@b.mx");
        }
        reloj.set(reloj.get().plus(Duration.ofMinutes(30)));

        control.registrarFallo("nuevo@b.mx"); // dispara la limpieza de las vencidas

        assertDoesNotThrow(() -> control.verificar("u1@b.mx"));
    }

    @Test
    void alLimpiarConEntradasVigentesSeConservan() {
        ControlIntentos control = new ControlIntentos();
        for (int i = 0; i < 10_000; i++) {
            control.registrarFallo("u" + i + "@b.mx");
        }
        control.registrarFallo("nuevo@b.mx"); // limpieza: ninguna vencida, no se descarta nada
        for (int i = 0; i < ControlIntentos.MAXIMO; i++) {
            control.registrarFallo("u1@b.mx");
        }
        assertThrows(DemasiadosIntentosException.class, () -> control.verificar("u1@b.mx"));
    }

    // ---- Clases vacías pendientes ----

    @Test
    void clasesPendientesSePuedenInstanciar() {
        assertNotNull(new RecuperarPasswordService());
        assertNotNull(new CodigoRecuperacion());
    }
}
