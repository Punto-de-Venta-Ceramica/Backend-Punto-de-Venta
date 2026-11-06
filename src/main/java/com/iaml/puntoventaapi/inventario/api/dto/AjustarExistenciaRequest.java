package com.iaml.puntoventaapi.inventario.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** cantidad > 0 suma existencias; cantidad < 0 las resta. */
public record AjustarExistenciaRequest(
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = -1_000_000, message = "El ajuste no puede ser menor a -1,000,000.")
        @Max(value = 1_000_000, message = "El ajuste no puede ser mayor a 1,000,000.") Integer cantidad) {

    @JsonIgnore
    @AssertTrue(message = "La cantidad debe ser distinta de cero.")
    public boolean isCantidadDistintaDeCero() {
        return cantidad == null || cantidad != 0;
    }
}
