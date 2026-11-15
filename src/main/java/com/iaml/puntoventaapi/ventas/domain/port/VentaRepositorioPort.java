package com.iaml.puntoventaapi.ventas.domain.port;

import com.iaml.puntoventaapi.ventas.domain.Venta;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.port.CajaPort.CorteAbierto;
import java.util.Optional;
import java.util.UUID;

public interface VentaRepositorioPort {
    VentaConfirmada guardar(Venta venta, CorteAbierto corte, UUID claveOperacion);
    Optional<VentaConfirmada> buscarPorOperacion(Long sucursalId, UUID claveOperacion);
    Optional<Long> buscarSucursalDeVenta(Long ventaId);
    Optional<VentaConfirmada> buscarPorId(Long ventaId);
}
