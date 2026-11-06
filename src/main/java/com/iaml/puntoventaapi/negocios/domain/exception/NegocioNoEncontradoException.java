package com.iaml.puntoventaapi.negocios.domain.exception;

public class NegocioNoEncontradoException extends RuntimeException {

    public NegocioNoEncontradoException() {
        super("El negocio no existe o no esta asociado a tu usuario");
    }
}
