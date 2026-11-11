package com.iaml.puntoventaapi.inventario.domain.port;

import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import java.util.List;
import java.util.Optional;

public interface InventarioRepositorioPort {

    /** Inserta si no tiene id; si lo tiene, actualiza la existencia. */
    Inventario guardar(Inventario inventario);

    /**
     * Busca un registro del usuario y lo bloquea hasta que termine la transacción actual,
     * para que dos ajustes simultáneos no se pisen. Debe llamarse dentro de una transacción.
     */
    Optional<Inventario> buscarParaAjuste(Long inventarioId, Long usuarioId);

    /** Negocio activo del usuario al que pertenece el producto activo; vacío si no es accesible. */
    Optional<Long> negocioDeProducto(Long productoId, Long usuarioId);

    /** Negocio activo del usuario al que pertenece la sucursal; vacío si no es accesible. */
    Optional<Long> negocioDeSucursal(Long sucursalId, Long usuarioId);

    boolean existe(Long sucursalId, Long productoId);

    /** Existencias de los productos activos del negocio; sucursalId es opcional. */
    List<ExistenciaProducto> listarPanel(Long negocioId, Long sucursalId, Long usuarioId);

    /** Existencias de un producto activo del usuario en cada sucursal; vacía si no es accesible. */
    List<ExistenciaProducto> listarPorProducto(Long productoId, Long usuarioId);
}
