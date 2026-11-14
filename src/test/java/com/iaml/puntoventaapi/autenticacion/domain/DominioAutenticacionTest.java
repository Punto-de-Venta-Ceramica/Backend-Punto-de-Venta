package com.iaml.puntoventaapi.autenticacion.domain;

import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DominioAutenticacionTest {

    private static final Rol ROL = new Rol(1L, "empleado", null);

    @Test
    void permisoExigeNombre() {
        assertEquals("ver", new Permiso(1L, "ver").nombre());
        assertThrows(IllegalArgumentException.class, () -> new Permiso(1L, null));
        assertThrows(IllegalArgumentException.class, () -> new Permiso(1L, "  "));
    }

    @Test
    void rolNormalizaNombreYPermisos() {
        Rol rol = new Rol(2L, "  propietario ", Set.of(new Permiso(1L, "ver")));

        assertEquals(2L, rol.getId());
        assertEquals("PROPIETARIO", rol.getNombre());
        assertTrue(rol.esPropietario());
        assertEquals(1, rol.getPermisos().size());
        assertFalse(ROL.esPropietario());
        assertTrue(ROL.getPermisos().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new Rol(1L, null, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Rol(1L, " ", Set.of()));
    }

    @Test
    void credencialExigeTodosLosDatos() {
        assertThrows(IllegalArgumentException.class, () -> new Credencial(null, "a", "h", ROL));
        assertThrows(IllegalArgumentException.class, () -> new Credencial(1L, null, "h", ROL));
        assertThrows(IllegalArgumentException.class, () -> new Credencial(1L, "a", null, ROL));
        assertThrows(IllegalArgumentException.class, () -> new Credencial(1L, "a", "h", null));
    }

    @Test
    void credencialVerificaLaPasswordYExponeSusDatos() {
        Credencial credencial = new Credencial(1L, "a@b.mx", "hash", ROL);

        assertEquals(1L, credencial.getUsuarioId());
        assertEquals("a@b.mx", credencial.getCorreo());
        assertSame(ROL, credencial.getRol());
        assertDoesNotThrow(() -> credencial.verificarPassword("hash", (plano, hash) -> plano.equals(hash)));
        assertThrows(CredencialesInvalidasException.class,
            () -> credencial.verificarPassword("otra", (plano, hash) -> plano.equals(hash)));
        assertThrows(CredencialesInvalidasException.class,
            () -> credencial.verificarPassword(null, (plano, hash) -> true));
    }

    @Test
    void laHuellaEsEstableYDependeDelHash() {
        Credencial a = new Credencial(1L, "a@b.mx", "hash-1", ROL);
        Credencial b = new Credencial(1L, "a@b.mx", "hash-2", ROL);

        assertEquals(16, a.huellaPassword().length());
        assertEquals(a.huellaPassword(), a.huellaPassword());
        assertNotEquals(a.huellaPassword(), b.huellaPassword());
    }

    @Test
    void laHuellaFallaSiElAlgoritmoNoExiste() {
        Credencial credencial = new Credencial(1L, "a@b.mx", "hash", ROL);

        assertThrows(IllegalStateException.class, () -> credencial.huellaPassword("NO-EXISTE"));
    }

    @Test
    void excepcionesTienenMensajeFijo() {
        assertEquals("Correo o contraseña incorrectos", new CredencialesInvalidasException().getMessage());
        assertTrue(new DemasiadosIntentosException().getMessage().startsWith("Demasiados intentos"));
    }
}
