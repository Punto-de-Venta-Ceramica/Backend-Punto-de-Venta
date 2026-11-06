package com.iaml.puntoventaapi.negocios.domain.port;

import com.iaml.puntoventaapi.negocios.domain.LogoArchivo;

public interface ProcesadorLogoPort {
    String procesar(LogoArchivo logo);
}
