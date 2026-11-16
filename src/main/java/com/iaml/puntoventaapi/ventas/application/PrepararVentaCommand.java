package com.iaml.puntoventaapi.ventas.application;

import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import java.math.BigDecimal;
import java.util.List;

public record PrepararVentaCommand(Long sucursalId, MetodoPago metodoPago,
                                   List<ProductoSolicitado> productos, BigDecimal montoRecibido) {
}
