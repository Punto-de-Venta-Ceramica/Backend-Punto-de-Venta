package com.iaml.puntoventaapi.pedidos.domain.port;

import com.iaml.puntoventaapi.pedidos.domain.Pedido;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido;
import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;

public interface PedidoRepositorioPort {
    record DatosRegistro(long sucursalId, long usuarioId, String descripcion,
                         String tipoEntrega, String descripcionEnvio, Long corteCajaId) { }
    record DatosEdicion(String descripcion, String tipoEntrega, String descripcionEnvio) { }
    /** Rechaza sucursales ajenas o inaccesibles al usuario autenticado. */
    void comprobarAcceso(long sucursalId, long usuarioId);
    long guardar(DatosRegistro datos, long clienteId, Pedido pedido);
    /** Verifica pertenencia y bloquea el pedido durante la transacción. */
    Pedido obtenerParaActualizar(long pedidoId, long usuarioId);
    /** Confirma el rol vigente y la pertenencia al negocio durante la transacción. */
    boolean esPropietarioActual(long pedidoId, long usuarioId);
    void actualizarDatos(long pedidoId, long clienteId, DatosEdicion datos);
    void guardarPago(long pedidoId, PagoPedido pago, long corteCajaId);
    void guardarEstado(long pedidoId, EstadoPedido estado);
}
