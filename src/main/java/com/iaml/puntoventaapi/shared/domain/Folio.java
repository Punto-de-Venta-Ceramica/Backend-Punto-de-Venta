package com.iaml.puntoventaapi.shared.domain;

/**
 * Folio que ve el cliente en tickets y reclamos: un prefijo que indica de qué se trata y el id con al menos
 * 6 dígitos (PED-000123 para un pedido, VTA-000123 para una venta). Es único porque el id lo es.
 */
public enum Folio {
    PEDIDO("PED"),
    VENTA("VTA");

    private final String prefijo;

    Folio(String prefijo) {
        this.prefijo = prefijo;
    }

    public String de(long id) {
        return String.format("%s-%06d", prefijo, id);
    }
}
