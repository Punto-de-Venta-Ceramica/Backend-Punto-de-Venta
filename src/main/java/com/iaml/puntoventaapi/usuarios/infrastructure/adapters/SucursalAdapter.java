package com.iaml.puntoventaapi.usuarios.infrastructure.adapters;

import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.usuarios.domain.port.SucursalPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SucursalAdapter implements SucursalPort {

    private final SucursalContrato sucursalContrato;

    @Override
    public boolean perteneceANegocio(Long sucursalId, Long negocioId) {
        return sucursalContrato.perteneceANegocio(sucursalId, negocioId);
    }

    @Override
    public Optional<String> obtenerNombrePorId(Long sucursalId) {
        return sucursalContrato.obtenerNombrePorId(sucursalId);
    }
}

