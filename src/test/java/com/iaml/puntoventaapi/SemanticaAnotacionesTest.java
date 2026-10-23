package com.iaml.puntoventaapi;

import jakarta.validation.Validation;
import jakarta.validation.constraints.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemanticaAnotacionesTest {
    // Example contract for explaining constraints; not an extra registration API.
    record Ejemplo(@NotNull Boolean activo, @NotNull Integer existencias,
                   @NotEmpty @Size(max = 3) List<@NotBlank String> permisos,
                   @NotEmpty String referencia) {}

    @Test void notNullPermiteFalseYCeroNotEmptyRequiereContenido() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new Ejemplo(false, 0, List.of("LEER"), " ")).isEmpty());
            assertEquals(4, validator.validate(new Ejemplo(null, null, List.of(), "")).size());
            assertFalse(validator.validate(new Ejemplo(true, 1, null, null)).isEmpty());
            assertFalse(validator.validate(new Ejemplo(true, 1, List.of(" "), "ref")).isEmpty());
            assertFalse(validator.validate(new Ejemplo(true, 1, List.of("A", "B", "C", "D"), "ref")).isEmpty());
        }
    }
}
