package com.iaml.puntoventaapi.negocios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import org.junit.jupiter.api.Test;

class NegocioEsloganTest {

    @Test
    void elEsloganSeRecortaYVacioQuedaSinEslogan() {
        assertEquals("La mejor calidad", Negocio.crear("Santo Barro", null, "  La mejor calidad ").getEslogan());
        assertNull(Negocio.crear("Santo Barro", null, "   ").getEslogan());
        assertNull(Negocio.crear("Santo Barro", null, null).getEslogan());
    }

    @Test
    void elEsloganNoPasaDe150Caracteres() {
        assertThrows(IllegalArgumentException.class,
            () -> Negocio.crear("Santo Barro", null, "x".repeat(Negocio.MAX_ESLOGAN + 1)));
    }

    @Test
    void editarCambiaElEsloganYEliminarLoConserva() {
        Negocio negocio = Negocio.crear("Santo Barro", null, "Viejo").editar("Santo Barro", null, "Nuevo");
        assertEquals("Nuevo", negocio.getEslogan());
        assertEquals("Nuevo", negocio.eliminar().getEslogan());
    }
}
