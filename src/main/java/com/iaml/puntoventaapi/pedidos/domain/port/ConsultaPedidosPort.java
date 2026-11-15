package com.iaml.puntoventaapi.pedidos.domain.port;

import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ConsultaPedidosPort {
    record Cliente(String nombre, String telefono, String ciudad, String colonia, String calle) { }
    /** imagenUrl: imagen actual del producto en el catálogo; null si no tiene. */
    record Detalle(long productoId, String nombre, int cantidad, BigDecimal precioUnitario,
                   BigDecimal porcentajeDescuento, BigDecimal subtotal, String imagenUrl) { }
    record Pago(long id, BigDecimal monto, String metodoPago, String tipoPago, Instant fecha) { }
    /** negocioEslogan: frase del negocio para el pie del ticket; null si no tiene. */
    record Vista(long id, long sucursalId, String folio, String descripcion,
                 String tipoEntrega, String descripcionEnvio, String estado, Instant fechaCreacion,
                 Cliente cliente, List<Detalle> productos, List<Pago> pagos,
                 BigDecimal total, BigDecimal totalPagado, BigDecimal saldoPendiente, boolean pagado,
                 String negocioEslogan) { }

    Vista consultar(long pedidoId, long usuarioId);
    /** Línea del listado: solo lo necesario para la tarjeta. */
    record ResumenProducto(String nombre, int cantidad) { }

    /** Fila del listado paginado. No incluye el folio: se obtiene al consultar el pedido. */
    record Resumen(long id, long sucursalId, String tipoEntrega, String estado, Instant fechaCreacion,
                   String cliente, List<ResumenProducto> productos,
                   BigDecimal total, BigDecimal totalPagado, BigDecimal saldoPendiente, boolean pagado) { }

    /** estado null = todos. pagina empieza en 0; el orden es siempre del más reciente al más antiguo. */
    Pagina<Resumen> listar(long sucursalId, long usuarioId, EstadoPedido estado, int pagina, int tamanio);
}
