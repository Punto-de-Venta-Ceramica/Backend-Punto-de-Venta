package com.iaml.puntoventaapi.pedidos.domain.port;

import java.math.BigDecimal;
import java.util.List;

/** Datos necesarios para capturar un pedido, limitados a la sucursal autorizada. */
public interface OpcionesPedidoPort {
    record Producto(long id, String nombre, BigDecimal precio, Integer cantidadMinima,
                    Integer cantidadMaxima, BigDecimal porcentajeDescuento) { }
    record Corte(long id, String apertura) { }
    record SucursalAsignada(long sucursalId, String sucursal, long negocioId, String negocio) { }
    List<Producto> productos(long sucursalId, long usuarioId);
    List<Corte> cortesAbiertos(long sucursalId, long usuarioId);
    java.util.Optional<SucursalAsignada> sucursalAsignada(long usuarioId);
}
