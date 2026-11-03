package com.iaml.puntoventaapi.catalogo.domain.exception;

import java.util.Map;

public class DatosCatalogoInvalidosException extends RuntimeException {

    private final Map<String, String> errores;

    public DatosCatalogoInvalidosException(Map<String, String> errores) {
        super("Revisa los datos del formulario.");
        this.errores = Map.copyOf(errores);
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
