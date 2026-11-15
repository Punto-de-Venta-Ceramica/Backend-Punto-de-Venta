package com.iaml.puntoventaapi.ventas.domain;

import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datos del carrito; los importes y descuentos se obtienen del catálogo, nunca del cliente. */
public record SolicitudVenta(Long sucursalId, MetodoPago metodoPago,
                             List<ProductoSolicitado> productos, BigDecimal montoRecibido) {

    public static final int MAX_PRODUCTOS = 200;
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("9999999999999999.99");

    public SolicitudVenta {
        Map<String, String> errores = new LinkedHashMap<>();
        if (sucursalId == null || sucursalId <= 0) {
            errores.put("sucursalId", "Selecciona una sucursal válida.");
        }
        if (productos == null || productos.isEmpty() || productos.size() > MAX_PRODUCTOS) {
            errores.put("productos", "La venta debe contener entre 1 y " + MAX_PRODUCTOS + " productos.");
        } else {
            Map<Long, Integer> cantidades = new LinkedHashMap<>();
            for (ProductoSolicitado producto : productos) {
                if (producto == null) {
                    errores.put("productos", "La lista de productos no puede contener elementos vacíos.");
                } else if (cantidades.putIfAbsent(producto.productoId(), producto.cantidad()) != null) {
                    errores.put("productos", "Cada producto debe aparecer una sola vez; actualiza su cantidad.");
                }
            }
        }
        if (montoRecibido != null) {
            if (metodoPago != MetodoPago.EFECTIVO) {
                errores.put("montoRecibido", "El monto recibido solo corresponde a pagos en efectivo.");
            } else if (montoRecibido.signum() < 0 || montoRecibido.scale() > 2
                    || montoRecibido.compareTo(MONTO_MAXIMO) > 0) {
                errores.put("montoRecibido", "El monto debe ser positivo o cero y admitir máximo 16 enteros y 2 decimales.");
            }
        }
        if (!errores.isEmpty()) {
            throw new DatosVentaInvalidosException(errores);
        }
        productos = List.copyOf(productos);
    }

    public Map<Long, Integer> cantidadesPorProducto() {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        productos.forEach(producto -> cantidades.put(producto.productoId(), producto.cantidad()));
        return Collections.unmodifiableMap(cantidades);
    }
}
