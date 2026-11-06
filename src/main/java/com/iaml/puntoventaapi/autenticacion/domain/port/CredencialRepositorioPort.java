package com.iaml.puntoventaapi.autenticacion.domain.port;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import java.util.Optional;

public interface CredencialRepositorioPort {

    Optional<Credencial> buscarPorCorreo(String correo);
}
