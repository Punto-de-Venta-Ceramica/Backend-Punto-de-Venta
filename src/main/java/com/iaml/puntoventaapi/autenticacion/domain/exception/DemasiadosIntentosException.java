package com.iaml.puntoventaapi.autenticacion.domain.exception;

public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException() {
        super("Demasiados intentos fallidos. Espera unos minutos antes de volver a intentarlo.");
    }
}
