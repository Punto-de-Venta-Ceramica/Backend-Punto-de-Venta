package com.iaml.puntoventaapi.pedidos.domain.port;

import com.iaml.puntoventaapi.pedidos.domain.EscalonDescuento;
import java.math.BigDecimal;
import java.util.List;

/** Datos necesarios para capturar un pedido, limitados a la sucursal autorizada. */
public interface OpcionesPedidoPort {
    /** descuentos: escalones por cantidad ordenados por cantidad mínima; vacío si el producto no tiene. */
    record Producto(long id, String nombre, BigDecimal precio, List<EscalonDescuento> descuentos) { }
    record Corte(long id, String apertura) { }
    record SucursalAsignada(long sucursalId, String sucursal, long negocioId, String negocio) { }
    List<Producto> productos(long sucursalId, long usuarioId);
    List<Corte> cortesAbiertos(long sucursalId, long usuarioId);
    List<SucursalAsignada> sucursalesAsignadas(long usuarioId);
}
