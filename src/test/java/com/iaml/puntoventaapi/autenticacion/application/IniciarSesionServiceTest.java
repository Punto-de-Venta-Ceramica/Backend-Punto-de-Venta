package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Rol;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IniciarSesionServiceTest {

    private static final Credencial CREDENCIAL =
        new Credencial(7L, "dueno@barro.mx", "hash-secreto", new Rol(1L, Rol.PROPIETARIO, Set.of()));

    // Puertos sustituidos por lambdas: el caso de uso se prueba sin BD ni Spring.
    private final IniciarSesionService service = new IniciarSesionService(
        correo -> correo.equals("dueno@barro.mx") ? Optional.of(CREDENCIAL) : Optional.empty(),
        (plano, hash) -> hash.equals("hash-" + plano),
        c -> new Sesion("token", Instant.EPOCH, c.getUsuarioId(), c.getRol().getNombre()));

    @Test
    void devuelveSesionConCredencialesCorrectas() {
        Sesion sesion = service.iniciarSesion(new IniciarSesionCommand("dueno@barro.mx", "secreto"));

        assertEquals(7L, sesion.usuarioId());
        assertEquals(Rol.PROPIETARIO, sesion.rol());
    }

    @Test
    void rechazaPasswordIncorrecta() {
        assertThrows(CredencialesInvalidasException.class,
            () -> service.iniciarSesion(new IniciarSesionCommand("dueno@barro.mx", "otra")));
    }

    @Test
    void rechazaCorreoInexistente() {
        assertThrows(CredencialesInvalidasException.class,
            () -> service.iniciarSesion(new IniciarSesionCommand("nadie@barro.mx", "secreto")));
    }
}
