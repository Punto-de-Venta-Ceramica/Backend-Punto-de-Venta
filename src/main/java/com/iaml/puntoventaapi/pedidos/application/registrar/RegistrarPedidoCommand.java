package com.iaml.puntoventaapi.pedidos.application.registrar;

import java.time.Instant;
import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido.MetodoPago;
import java.math.BigDecimal;
import java.util.List;

public record RegistrarPedidoCommand(long sucursalId, long usuarioId,
        String descripcion, ClientePedido cliente, List<ProductoSolicitado> productos,
        String tipoEntrega, String descripcionEnvio, BigDecimal montoInicial, MetodoPago metodoPago,
        Instant fechaEntrega) {
    /**
     * aplicarDescuento: si se usa el descuento vigente del producto (por jerarquía:
     * producto, categoría, área). El vendedor puede decidir no aplicarlo.
     * descuentoManual: porcentaje (1 a 100) capturado por el vendedor; si viene, reemplaza al del catálogo.
     */
    public record ProductoSolicitado(long productoId, int cantidad, boolean aplicarDescuento,
                                     BigDecimal descuentoManual) {
        public ProductoSolicitado(long productoId, int cantidad, boolean aplicarDescuento) {
            this(productoId, cantidad, aplicarDescuento, null);
        }

        public ProductoSolicitado(long productoId, int cantidad) {
            this(productoId, cantidad, true);
        }
    }
}
