package com.iaml.puntoventaapi.sucursal;

import com.iaml.puntoventaapi.sucursal.infrastructure.security.UsuarioAutenticadoAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class UsuarioAutenticadoAdapterTest {

    private final UsuarioAutenticadoAdapter adapter = new UsuarioAutenticadoAdapter();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtieneElIdDelSubjectDelJwtAutenticado() {
        autenticar("27");
        assertThat(adapter.obtenerId()).isEqualTo(27L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "0", "-1", "otro", "1.5", "99999999999999999999999"})
    void unSubjectInvalidoNoSeConvierteEnUsuario(String subject) {
        autenticar(subject);
        assertThatThrownBy(adapter::obtenerId).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void exigeUnJwtYNoAceptaOtraClaseDeAutenticacion() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(adapter::obtenerId).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("27", "", List.of()));
        assertThatThrownBy(adapter::obtenerId).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private void autenticar(String subject) {
        Jwt.Builder builder = Jwt.withTokenValue("token-de-pruebas").header("alg", "HS256").claim("prueba", true);
        if (subject != null) {
            builder.subject(subject);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build(), List.of()));
    }
}

