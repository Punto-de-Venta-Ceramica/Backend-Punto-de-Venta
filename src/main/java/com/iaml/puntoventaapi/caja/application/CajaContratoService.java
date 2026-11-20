package com.iaml.puntoventaapi.caja.application;

import com.iaml.puntoventaapi.caja.CajaContrato;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.caja.infrastructure.persistence.CorteCajaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CajaContratoService implements CajaContrato {

    private final CorteCajaRepository corteCajaRepository;

    public CajaContratoService(CorteCajaRepository corteCajaRepository) {
        this.corteCajaRepository = corteCajaRepository;
    }

    @Override
    public Optional<Long> obtenerCorteAbiertoId(Long sucursalId) {
        return corteCajaRepository.findByCaja_Sucursal_IdAndFechaHoraCierreIsNull(sucursalId)
            .map(CorteCaja::getId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<CorteAbierto> bloquearCorteAbierto(Long sucursalId) {
        return corteCajaRepository.buscarIdAbierto(sucursalId)
            .flatMap(corteCajaRepository::buscarConBloqueo)
            .filter(corte -> corte.getFechaHoraCierre() == null)
            .map(corte -> new CorteAbierto(corte.getCaja().getId(), corte.getId()));
    }

    @Override
    public boolean hayCorteAbierto(Long sucursalId) {
        return corteCajaRepository.existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(sucursalId);
    }
}

