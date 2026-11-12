package com.iaml.puntoventaapi.pedidos.domain.port;

import com.iaml.puntoventaapi.pedidos.domain.EscalonDescuento;
import java.math.BigDecimal;
import java.util.List;

/** Obtiene datos del catálogo para la sucursal; nunca recibe precios del cliente HTTP. */
public interface CatalogoPort {
    /** escalones: descuentos por volumen vigentes del producto (vacío si no tiene). */
    record Producto(BigDecimal precio, List<EscalonDescuento> escalones) { }
    Producto consultar(long sucursalId, long usuarioId, long productoId);
}
