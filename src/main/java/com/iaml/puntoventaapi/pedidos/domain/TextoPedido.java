package com.iaml.puntoventaapi.pedidos.domain;

/** Normaliza los espacios de los textos capturados en un pedido. */
public final class TextoPedido {
    private TextoPedido() { }

    public static String normalizar(String valor) {
        if (valor == null) return null;
        var limpio = valor.replaceAll("[\\p{Z}\\s]+", " ").trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public static boolean contieneControl(String valor) {
        return valor != null && valor.codePoints().anyMatch(Character::isISOControl);
    }
}
