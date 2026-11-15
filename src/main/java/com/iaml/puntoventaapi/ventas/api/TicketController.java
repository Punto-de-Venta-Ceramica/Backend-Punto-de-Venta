package com.iaml.puntoventaapi.ventas.api;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.ventas.api.dto.VentaConfirmadaResponse;
import com.iaml.puntoventaapi.ventas.application.GenerarTicketService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas/{ventaId}/ticket")
public class TicketController {
    private final GenerarTicketService tickets;
    private final UsuarioActual usuarioActual;

    public TicketController(GenerarTicketService tickets, UsuarioActual usuarioActual) {
        this.tickets = tickets;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public VentaConfirmadaResponse consultar(@PathVariable @Positive Long ventaId) {
        return VentaConfirmadaResponse.from(tickets.consultar(ventaId, usuarioActual.id()));
    }
}
