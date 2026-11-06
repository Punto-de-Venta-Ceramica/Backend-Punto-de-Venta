package com.iaml.puntoventaapi.autenticacion.domain.port;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;

public interface TokenPort {

    Sesion generar(Credencial credencial);
}
