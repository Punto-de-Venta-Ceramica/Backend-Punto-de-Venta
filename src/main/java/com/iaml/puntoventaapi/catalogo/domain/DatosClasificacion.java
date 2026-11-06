package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.catalogo.domain.exception.DatosCatalogoInvalidosException;
import java.util.Map;

/** Reglas compartidas del nombre y descripción, independientes de HTTP y JPA. */
public record DatosClasificacion(String nombre, String descripcion) {

    public static final int MAX_NOMBRE = 100;

    public DatosClasificacion {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosCatalogoInvalidosException(Map.of("nombre", "El nombre es obligatorio."));
        }
        if (nombre.length() > MAX_NOMBRE) {
            throw new DatosCatalogoInvalidosException(
                Map.of("nombre", "El nombre no debe superar 100 caracteres."));
        }
        if (nombre.codePoints().anyMatch(Character::isISOControl)) {
            throw new DatosCatalogoInvalidosException(
                Map.of("nombre", "El nombre no puede contener saltos de línea ni caracteres de control."));
        }
        nombre = nombre.strip();
        descripcion = descripcion == null ? null : descripcion.strip();
        if (descripcion != null && descripcion.isEmpty()) {
            descripcion = null;
        }
    }
}
