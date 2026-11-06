package com.iaml.puntoventaapi.negocios.application;

import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;

public record EditarNegocioCommand(String nombre, LogoArchivo logo, boolean eliminarLogo) {
}
