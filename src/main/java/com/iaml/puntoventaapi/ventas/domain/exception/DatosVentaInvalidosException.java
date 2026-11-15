package com.iaml.puntoventaapi.ventas.domain.exception;

import java.util.Map;

public class DatosVentaInvalidosException extends RuntimeException {

    private final Map<String, String> errores;

    public DatosVentaInvalidosException(Map<String, String> errores) {
        super("Revisa los datos de la venta.");
        this.errores = Map.copyOf(errores);
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
