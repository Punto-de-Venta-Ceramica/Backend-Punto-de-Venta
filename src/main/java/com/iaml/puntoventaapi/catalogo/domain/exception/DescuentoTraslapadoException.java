package com.iaml.puntoventaapi.catalogo.domain.exception;

/**
 * El rango de piezas de un descuento activo se cruza con el de otro descuento activo del mismo producto,
 * categoría o área. Se responde 409 con el código {@link #CODIGO} para que el cliente lo identifique.
 */
public class DescuentoTraslapadoException extends DescuentoInvalidoException {

    public static final String CODIGO = "DESCUENTO_TRASLAPADO";

    public DescuentoTraslapadoException(String mensaje) {
        super(mensaje);
    }
}
