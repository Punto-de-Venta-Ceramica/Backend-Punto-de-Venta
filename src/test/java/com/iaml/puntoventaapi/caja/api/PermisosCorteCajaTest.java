package com.iaml.puntoventaapi.caja.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/** HU-05: el empleado opera el corte de su turno; los cortes anteriores son del propietario. */
class PermisosCorteCajaTest {

    private static PreAuthorize permiso(String metodo) {
        Method m = Arrays.stream(CorteCajaController.class.getDeclaredMethods())
            .filter(x -> x.getName().equals(metodo)).findFirst().orElseThrow();
        return m.getAnnotation(PreAuthorize.class);
    }

    @Test
    void elHistorialYLosCortesAnterioresPermitenPropietarioYEmpleado() {
        assertEquals("hasAnyRole('PROPIETARIO', 'EMPLEADO')", permiso("listarHistorial").value());
        assertEquals("hasAnyRole('PROPIETARIO', 'EMPLEADO')", permiso("consultarCorte").value());
    }

    @Test
    void elEmpleadoAbreConsultaElActivoYCierraSuCorte() {
        assertEquals("hasAnyRole('PROPIETARIO', 'EMPLEADO')",
            CorteCajaController.class.getAnnotation(PreAuthorize.class).value());
        assertNull(permiso("abrirCorte"));
        assertNull(permiso("obtenerCorteActivo"));
        assertNull(permiso("cerrarCorte"));
    }
}
