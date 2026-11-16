package com.iaml.puntoventaapi.ventas.application;

import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.exception.RecursoVentaNoEncontradoException;
import com.iaml.puntoventaapi.ventas.domain.port.SucursalPort;
import com.iaml.puntoventaapi.ventas.domain.port.VentaRepositorioPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerarTicketService {
    private final VentaRepositorioPort ventas;
    private final SucursalPort sucursales;

    public GenerarTicketService(VentaRepositorioPort ventas, SucursalPort sucursales) {
        this.ventas = ventas;
        this.sucursales = sucursales;
    }

    @Transactional(readOnly = true)
    public VentaConfirmada consultar(Long ventaId, Long usuarioId) {
        ventas.buscarSucursalDeVenta(ventaId)
            .flatMap(sucursalId -> sucursales.obtenerNegocioIdAccesible(sucursalId, usuarioId))
            .orElseThrow(GenerarTicketService::ventaNoEncontrada);
        return ventas.buscarPorId(ventaId).orElseThrow(GenerarTicketService::ventaNoEncontrada);
    }

    private static RecursoVentaNoEncontradoException ventaNoEncontrada() {
        return new RecursoVentaNoEncontradoException("La venta no existe o no tienes acceso.");
    }
}
