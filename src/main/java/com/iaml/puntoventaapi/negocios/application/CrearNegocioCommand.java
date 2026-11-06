package com.iaml.puntoventaapi.negocios.application;

import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;

public record CrearNegocioCommand(String nombre, LogoArchivo logo) {
}
