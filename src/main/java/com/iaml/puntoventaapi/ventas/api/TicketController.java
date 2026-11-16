package com.iaml.puntoventaapi.ventas.api;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.ventas.api.dto.VentaConfirmadaResponse;
import com.iaml.puntoventaapi.ventas.application.EsloganTicketService;
import com.iaml.puntoventaapi.ventas.application.GenerarTicketService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-05: quien registra la venta (propietario o empleado) puede consultar su ticket. */
@RestController
@RequestMapping("/api/ventas/{ventaId}/ticket")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class TicketController {
    private final GenerarTicketService tickets;
    private final EsloganTicketService eslogan;
    private final UsuarioActual usuarioActual;

    public TicketController(GenerarTicketService tickets, EsloganTicketService eslogan,
                            UsuarioActual usuarioActual) {
        this.tickets = tickets;
        this.eslogan = eslogan;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public VentaConfirmadaResponse consultar(@PathVariable @Positive Long ventaId) {
        var confirmada = tickets.consultar(ventaId, usuarioActual.id());
        return VentaConfirmadaResponse.from(confirmada, eslogan.de(confirmada));
    }
}
