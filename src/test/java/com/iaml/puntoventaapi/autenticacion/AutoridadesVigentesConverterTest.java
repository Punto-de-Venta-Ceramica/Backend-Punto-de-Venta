package com.iaml.puntoventaapi.autenticacion;

import static org.junit.jupiter.api.Assertions.*;
import com.iaml.puntoventaapi.autenticacion.domain.port.AutoridadesVigentesPort;
import com.iaml.puntoventaapi.autenticacion.infrastructure.security.AutoridadesVigentesConverter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class AutoridadesVigentesConverterTest {
    private Jwt token(String sujeto, String rol) {
        return Jwt.withTokenValue("test").header("alg", "HS256").subject(sujeto)
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
            .claim("rol", rol).claim("autoridades", List.of("ROLE_PROPIETARIO", "PERMISO_ANTIGUO"))
            .build();
    }

    @Test
    void usaPermisosActualesEnVezDeLosGuardadosEnElToken() {
        var converter = new AutoridadesVigentesConverter(id -> Optional.of(
            new AutoridadesVigentesPort.Autoridades("EMPLEADO", List.of("REGISTRAR_PEDIDO"))));
        var resultado = converter.convert(token("7", "EMPLEADO"));
        var permisos = resultado.getAuthorities().stream().map(Object::toString).toList();
        assertTrue(permisos.contains("ROLE_EMPLEADO"));
        assertTrue(permisos.contains("REGISTRAR_PEDIDO"));
        assertFalse(permisos.contains("ROLE_PROPIETARIO"));
        assertFalse(permisos.contains("PERMISO_ANTIGUO"));
    }

    @Test
    void rechazaSesionSiElRolCambioOElUsuarioYaNoExiste() {
        var cambio = new AutoridadesVigentesConverter(id -> Optional.of(
            new AutoridadesVigentesPort.Autoridades("EMPLEADO", List.of())));
        assertThrows(InvalidBearerTokenException.class, () -> cambio.convert(token("7", "PROPIETARIO")));
        var eliminado = new AutoridadesVigentesConverter(id -> Optional.empty());
        assertThrows(InvalidBearerTokenException.class, () -> eliminado.convert(token("7", "EMPLEADO")));
        assertThrows(InvalidBearerTokenException.class, () -> eliminado.convert(token("invalido", "EMPLEADO")));
    }
}
