package com.iaml.puntoventaapi.pedidos.domain;

import java.util.regex.Pattern;

/** Datos mínimos que HU-09 solicita al asociar un cliente al pedido. */
public record ClientePedido(String nombre, String telefono, String ciudad, String colonia,
                            String calle) {
    private static final Pattern NOMBRE_VALIDO = Pattern.compile(
        "^[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*$");
    private static final Pattern TELEFONO_VALIDO = Pattern.compile("^[0-9]{10}$");

    public ClientePedido {
        nombre = requerido(nombre, 150, "nombre");
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("El nombre debe contener letras y espacios válidos.");
        }
        if (telefono == null || !TELEFONO_VALIDO.matcher(telefono).matches()) {
            throw new IllegalArgumentException("El teléfono debe contener exactamente 10 dígitos.");
        }
        ciudad = opcional(ciudad, 120, "ciudad");
        colonia = opcional(colonia, 120, "colonia");
        calle = opcional(calle, 200, "calle");
    }

    private static String requerido(String valor, int maximo, String campo) {
        var limpio = TextoPedido.normalizar(valor);
        if (limpio == null || limpio.length() > maximo || TextoPedido.contieneControl(limpio)) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio y debe ser válido.");
        }
        return limpio;
    }

    private static String opcional(String valor, int maximo, String campo) {
        var limpio = TextoPedido.normalizar(valor);
        if (limpio == null) return null;
        if (TextoPedido.contieneControl(limpio)) {
            throw new IllegalArgumentException("El campo " + campo + " contiene caracteres no válidos.");
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " no debe superar " + maximo + " caracteres.");
        }
        return limpio;
    }
}
