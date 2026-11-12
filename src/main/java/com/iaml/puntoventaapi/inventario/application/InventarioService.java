package com.iaml.puntoventaapi.inventario.application;

import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioYaExisteException;
import com.iaml.puntoventaapi.inventario.domain.exception.ProductoOSucursalNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.UltimaSucursalException;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import com.iaml.puntoventaapi.inventario.domain.port.NegocioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepositorioPort inventarios;
    private final NegocioPort negocios;

    @Transactional(readOnly = true)
    public List<ExistenciaProducto> consultarPanel(Long negocioId, Long sucursalId, Long usuarioId) {
        // Un negocio ajeno y uno inexistente producen la misma respuesta pública. El empleado solo consulta
        // una sucursal suya; el panel de todo el negocio es del propietario.
        boolean permitido = sucursalId == null
            ? negocios.perteneceAUsuario(negocioId, usuarioId)
            : negocios.puedeOperarSucursal(negocioId, sucursalId, usuarioId);
        if (!permitido) {
            throw new NegocioNoEncontradoException();
        }
        return inventarios.listarPanel(negocioId, sucursalId, usuarioId);
    }

    /**
     * Existencias de un solo producto (detalle). Un producto ajeno o inexistente devuelve una lista vacía,
     * igual que uno sin sucursales asignadas: no revela si existe.
     */
    @Transactional(readOnly = true)
    public List<ExistenciaProducto> consultarPorProducto(Long productoId, Long usuarioId) {
        return inventarios.listarPorProducto(productoId, usuarioId);
    }

    /**
     * Asigna un producto ya registrado a otra sucursal del mismo negocio. Un producto o una sucursal
     * ajenos o inexistentes producen la misma respuesta pública. Si el producto se había quitado de esa
     * sucursal, se reactiva su registro: sin existencia indicada (null) conserva la que tenía.
     */
    @Transactional
    public Inventario asignar(Long productoId, Long sucursalId, Integer existencia, Long usuarioId) {
        Long negocioProducto = inventarios.negocioDeProducto(productoId, usuarioId)
            .orElseThrow(() -> new ProductoOSucursalNoEncontradoException());
        Long negocioSucursal = inventarios.negocioDeSucursal(sucursalId, usuarioId)
            .orElseThrow(() -> new ProductoOSucursalNoEncontradoException());
        if (!negocioProducto.equals(negocioSucursal)) {
            throw new ProductoOSucursalNoEncontradoException();
        }
        Inventario previo = inventarios.buscarPorSucursalYProducto(sucursalId, productoId).orElse(null);
        if (previo == null) {
            return inventarios.guardar(new Inventario(null, sucursalId, productoId,
                existencia == null ? 0 : existencia));
        }
        if (previo.isActivo()) {
            throw new InventarioYaExisteException();
        }
        previo.reactivar(existencia);
        return inventarios.guardar(previo);
    }

    /**
     * Quita el producto de una sucursal (baja lógica): la existencia se conserva por si se vuelve a asignar.
     * El producto debe seguir asignado al menos a otra sucursal.
     */
    @Transactional
    public void quitar(Long inventarioId, Long usuarioId) {
        Inventario inventario = inventarios.buscarParaQuitar(inventarioId, usuarioId)
            .orElseThrow(() -> new InventarioNoEncontradoException());
        if (inventarios.contarActivosDeProducto(inventario.getProductoId()) <= 1) {
            throw new UltimaSucursalException();
        }
        inventario.desactivar();
        inventarios.guardar(inventario);
    }

    /** El bloqueo de la fila dura hasta que termina esta transacción: lectura, regla y guardado son atómicos. */
    @Transactional
    public Inventario ajustar(Long inventarioId, int cantidad, Long usuarioId) {
        Inventario inventario = inventarios.buscarParaAjuste(inventarioId, usuarioId)
            .orElseThrow(() -> new InventarioNoEncontradoException());
        inventario.ajustar(cantidad);
        return inventarios.guardar(inventario);
    }
}
