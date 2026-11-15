package com.iaml.puntoventaapi.autenticacion.infrastructure.security;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Rol;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.RefreshTokenPort.RefreshValido;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenAdapterTest {

    private static final String SECRETO = "0123456789abcdef0123456789abcdef-secreto";
    private static final Credencial CREDENCIAL =
        new Credencial(7L, "dueno@barro.mx", "hash", new Rol(1L, Rol.PROPIETARIO, Set.of()));

    private final SecurityConfig config = new SecurityConfig(SECRETO);
    private final JwtTokenAdapter adapter = new JwtTokenAdapter(config.jwtEncoder(), SECRETO, 15, 20);
    private final JwtDecoder decoderApi = config.jwtDecoder();

    @Test
    void accesoDura15MinutosYRenovacion20Dias() {
        Sesion sesion = adapter.generar(CREDENCIAL);

        assertTrue(Duration.between(Instant.now(), sesion.expiraEn()).toMinutes() <= 15);
        assertTrue(Duration.between(Instant.now(), sesion.expiraEn()).toMinutes() >= 14);
        assertTrue(Duration.between(Instant.now(), sesion.refreshExpiraEn()).toDays() >= 19);
    }

    @Test
    void laApiAceptaElAccesoYRechazaLaRenovacion() {
        Sesion sesion = adapter.generar(CREDENCIAL);

        Jwt acceso = decoderApi.decode(sesion.token());
        assertEquals("7", acceso.getSubject());
        assertThrows(JwtException.class, () -> decoderApi.decode(sesion.refreshToken()));
    }

    @Test
    void renovarConservaElVencimientoDelRefreshYNoAceptaUnAcceso() {
        Sesion inicial = adapter.generar(CREDENCIAL);

        RefreshValido valido = adapter.validar(inicial.refreshToken());
        assertEquals(7L, valido.usuarioId());
        Sesion renovada = adapter.renovarAcceso(CREDENCIAL, inicial.refreshToken(), valido.expiraEn());
        assertEquals(inicial.refreshToken(), renovada.refreshToken());
        assertEquals(valido.expiraEn(), renovada.refreshExpiraEn());
        decoderApi.decode(renovada.token());

        assertThrows(CredencialesInvalidasException.class, () -> adapter.validar(inicial.token()));
        assertThrows(CredencialesInvalidasException.class, () -> adapter.validar("basura"));
    }

    @Test
    void elTokenDeRenovacionDejaDeServirSiCambiaLaPassword() {
        Credencial original = new Credencial(7L, "dueno@barro.mx", "hash", new Rol(1L, Rol.PROPIETARIO, Set.of()));
        Credencial conOtraPassword =
            new Credencial(7L, "dueno@barro.mx", "hash-nuevo", new Rol(1L, Rol.PROPIETARIO, Set.of()));

        RefreshValido valido = adapter.validar(adapter.generar(original).refreshToken());

        org.junit.jupiter.api.Assertions.assertTrue(valido.vigentePara(original));
        org.junit.jupiter.api.Assertions.assertFalse(valido.vigentePara(conOtraPassword));
        // Los tokens emitidos antes de que existiera la huella se siguen aceptando hasta que venzan.
        org.junit.jupiter.api.Assertions.assertTrue(
            new RefreshValido(7L, valido.expiraEn()).vigentePara(conOtraPassword));
    }
}
