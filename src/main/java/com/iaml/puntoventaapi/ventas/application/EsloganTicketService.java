package com.iaml.puntoventaapi.ventas.application;

import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.port.NegocioPort;
import org.springframework.stereotype.Service;

/** Eslogan del negocio que se imprime al pie del ticket de venta; null si no tiene. */
@Service
public class EsloganTicketService {
    private final NegocioPort negocios;

    public EsloganTicketService(NegocioPort negocios) {
        this.negocios = negocios;
    }

    public String de(VentaConfirmada confirmada) {
        return negocios.eslogan(confirmada.venta().negocioId()).orElse(null);
    }
}
