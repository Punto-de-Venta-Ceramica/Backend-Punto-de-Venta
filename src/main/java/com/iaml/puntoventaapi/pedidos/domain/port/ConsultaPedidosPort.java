package com.iaml.puntoventaapi.pedidos.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ConsultaPedidosPort {
    record Cliente(String nombre, String telefono, String ciudad, String colonia, String calle) { }
    record Detalle(long productoId, String nombre, int cantidad, BigDecimal precioUnitario,
                   BigDecimal porcentajeDescuento, BigDecimal subtotal) { }
    record Pago(long id, BigDecimal monto, String metodoPago, String tipoPago, Instant fecha) { }
    record Vista(long id, long sucursalId, String folio, String descripcion,
                 String tipoEntrega, String descripcionEnvio, String estado, Instant fechaCreacion,
                 Cliente cliente, List<Detalle> productos, List<Pago> pagos,
                 BigDecimal total, BigDecimal totalPagado, BigDecimal saldoPendiente, boolean pagado) { }

    Vista consultar(long pedidoId, long usuarioId);
    List<Vista> listar(long sucursalId, long usuarioId);
}
