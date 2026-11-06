package com.iaml.puntoventaapi.pedidos.domain;

/** Datos mínimos que HU-09 solicita al asociar un cliente al pedido. */
public record ClientePedido(String nombre, String telefono, String ciudad, String colonia,
                            String calle) {
    public ClientePedido {
        nombre = requerido(nombre, 150, "nombre");
        telefono = requerido(telefono, 30, "teléfono");
        ciudad = requerido(ciudad, 120, "ciudad");
        colonia = requerido(colonia, 120, "colonia");
        calle = requerido(calle, 200, "calle");
    }

    private static String requerido(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio y debe ser válido.");
        }
        return valor.trim();
    }
}
