package com.iaml.puntoventaapi.ventas.infrastructure.adapters;

import com.iaml.puntoventaapi.caja.CajaContrato;
import com.iaml.puntoventaapi.ventas.domain.port.CajaPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CajaAdapter implements CajaPort {
    private final CajaContrato caja;

    public CajaAdapter(CajaContrato caja) {
        this.caja = caja;
    }

    @Override
    public Optional<CorteAbierto> bloquearCorteAbierto(Long sucursalId) {
        return caja.bloquearCorteAbierto(sucursalId)
            .map(corte -> new CorteAbierto(corte.cajaId(), corte.corteId()));
    }
}
