package com.iaml.puntoventaapi.negocios.application;

import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;

/** eslogan null = conservar el actual; vacío = quitarlo. */
public record EditarNegocioCommand(String nombre, LogoArchivo logo, boolean eliminarLogo, String eslogan) {
}
