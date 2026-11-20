package com.iaml.puntoventaapi.inventario;

import java.util.Map;
import java.util.Optional;
import com.iaml.puntoventaapi.inventario.domain.ProductoEnSucursal;
import com.iaml.puntoventaapi.shared.domain.FiltroProductosVenta;
import com.iaml.puntoventaapi.shared.domain.Pagina;

public interface InventarioContrato {

    /**
     * Crea el registro de inventario de un producto recién creado en cada sucursal indicada.
     * Participa en la transacción del llamador.
     *
     * @param existenciaPorSucursal id de sucursal -> existencia inicial (>= 0)
     */
    void registrarExistenciasIniciales(Long productoId, Map<Long, Integer> existenciaPorSucursal);

    /** Consulta disponibilidad sin reservar ni modificar las existencias. */
    Map<Long, Integer> consultarExistencias(Long negocioId, Long sucursalId, Long usuarioId);

    /** Consulta paginada para una sucursal previamente autorizada. */
    Pagina<ProductoEnSucursal> listarProductosParaVenta(Long negocioId, Long sucursalId,
                                                         FiltroProductosVenta filtros, int pagina, int tamanio);

    /** Operación interna para una sucursal ya autorizada por el caso de uso de ventas. */
    Optional<FaltanteVenta> descontarParaVenta(Long sucursalId, Map<Long, Integer> cantidades);

    record FaltanteVenta(Long productoId, int solicitada, int disponible) { }
}
