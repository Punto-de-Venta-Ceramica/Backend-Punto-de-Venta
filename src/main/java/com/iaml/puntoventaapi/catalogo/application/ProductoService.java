package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.InventarioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.SucursalPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private static final int NOMBRE_DESCUENTO_MAX = 120;

    private final ProductoRepositorioPort productos;
    private final CategoriaRepositorioPort categorias;
    private final DescuentoRepositorioPort descuentos;
    private final NegocioPort negocios;
    private final SucursalPort sucursales;
    private final InventarioPort inventario;

    /** Producto, descuento (opcional) e inventario por sucursal se guardan en una sola transacción. */
    @Transactional
    public ProductoDetalle crear(CrearProductoCommand command, Long usuarioId) {
        Long negocioId = resolverNegocioDeCategoria(command.categoriaId(), usuarioId);
        Map<Long, Integer> existencias = resolverExistencias(negocioId, command.existencias(), usuarioId);

        Producto producto = productos.guardar(new Producto(null, command.categoriaId(), command.nombre(),
            command.precio(), true));
        // Sin descuento propio, el producto puede heredar el de su categoría o área.
        Descuento descuento = command.descuento() == null
            ? descuentos.buscarActivoPorProducto(producto.getId()).orElse(null)
            : crearDescuento(producto, negocioId, command.descuento());
        inventario.registrarExistenciasIniciales(producto.getId(), existencias);
        return new ProductoDetalle(producto, descuento);
    }

    /** Nunca modifica la existencia: eso es responsabilidad del módulo de inventario. */
    @Transactional
    public ProductoDetalle editar(EditarProductoCommand command, Long usuarioId) {
        Producto producto = buscarProducto(command.id(), usuarioId);
        Long categoriaAnterior = producto.getCategoriaId();
        Long negocioId = categorias.buscarNegocioId(categoriaAnterior)
            .orElseThrow(() -> new CategoriaNoEncontradaException());

        producto.setNombre(command.nombre());
        producto.setPrecio(command.precio());
        producto.setCategoriaId(command.categoriaId());
        Long negocioNuevo = resolverNegocioDeCategoria(producto.getCategoriaId(), usuarioId);
        if (!negocioNuevo.equals(negocioId)) {
            throw new ProductoInvalidoException("La categoría debe pertenecer al mismo negocio del producto.");
        }

        Producto guardado = productos.guardar(producto);
        return new ProductoDetalle(guardado, aplicarDescuento(guardado, negocioId, command.descuento()));
    }

    /** Baja lógica: el producto sigue referenciado por ventas, pedidos e inventario. */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Producto producto = buscarProducto(id, usuarioId);
        producto.desactivar();
        productos.guardar(producto);
    }

    @Transactional(readOnly = true)
    public ProductoDetalle consultar(Long id, Long usuarioId) {
        Producto producto = buscarProducto(id, usuarioId);
        return new ProductoDetalle(producto, descuentos.buscarActivoPorProducto(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ProductoDetalle> listar(Long negocioId, Long usuarioId) {
        // Un negocio ajeno y uno inexistente producen la misma respuesta pública.
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new NegocioNoEncontradoException();
        }
        List<Producto> lista = productos.listarPorNegocio(negocioId);
        Map<Long, Descuento> descuentosPorProducto =
            descuentos.buscarActivosPorProductos(lista.stream().map(Producto::getId).toList());
        return lista.stream().map(p -> new ProductoDetalle(p, descuentosPorProducto.get(p.getId()))).toList();
    }

    private Producto buscarProducto(Long id, Long usuarioId) {
        return productos.buscarPorId(id, usuarioId).orElseThrow(() -> new ProductoNoEncontradoException());
    }

    /** Una categoría ajena y una inexistente producen la misma respuesta pública. */
    private Long resolverNegocioDeCategoria(Long categoriaId, Long usuarioId) {
        Long negocioId = categorias.buscarNegocioIdParaActualizar(categoriaId)
            .orElseThrow(() -> new CategoriaNoEncontradaException());
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new CategoriaNoEncontradaException();
        }
        return negocioId;
    }

    /**
     * Un negocio con una sola sucursal asigna el producto a ella por defecto; con varias,
     * el propietario debe elegir en cuáles estará disponible.
     */
    private Map<Long, Integer> resolverExistencias(Long negocioId, List<ExistenciaInicialCommand> solicitadas,
                                                   Long usuarioId) {
        List<Long> sucursalIds = sucursales.idsPorNegocio(negocioId, usuarioId);
        if (sucursalIds.isEmpty()) {
            throw new ProductoInvalidoException(
                "El negocio no tiene sucursales. Registra una sucursal antes de crear productos.");
        }
        if (solicitadas == null || solicitadas.isEmpty()) {
            if (sucursalIds.size() == 1) {
                return Map.of(sucursalIds.get(0), 0);
            }
            throw new ProductoInvalidoException(
                "El negocio tiene varias sucursales: indica en cuáles estará disponible el producto.");
        }
        Map<Long, Integer> existencias = new LinkedHashMap<>();
        for (ExistenciaInicialCommand solicitada : solicitadas) {
            Long sucursalId = solicitada.sucursalId();
            if (sucursalId == null || !sucursalIds.contains(sucursalId)) {
                throw new ProductoInvalidoException("Alguna de las sucursales indicadas no pertenece al negocio.");
            }
            if (solicitada.existencia() == null || solicitada.existencia() < 0) {
                throw new ProductoInvalidoException("La existencia inicial no puede ser negativa.");
            }
            if (existencias.putIfAbsent(sucursalId, solicitada.existencia()) != null) {
                throw new ProductoInvalidoException("Hay sucursales repetidas en la lista de existencias.");
            }
        }
        return existencias;
    }

    private Descuento crearDescuento(Producto producto, Long negocioId, DescuentoProductoCommand datos) {
        Descuento nuevo = new Descuento(null, negocioId, nombreDescuento(producto), datos.cantidadMinima(),
            datos.cantidadMaxima(), datos.porcentaje(), true);
        Descuento guardado = descuentos.guardar(nuevo);
        descuentos.vincularAProducto(guardado.getId(), producto.getId());
        return guardado;
    }

    /**
     * Compatibilidad con clientes que mandan un único descuento en el producto: se edita su descuento propio
     * (el asignado únicamente a este producto) o se crea uno. Un descuento compartido con otros productos,
     * categorías o áreas no se modifica aquí: si el producto quiere uno distinto, se le crea uno propio, que
     * tiene prioridad sobre los de su categoría o área. Sin datos (null) no se toca ningún descuento.
     */
    private Descuento aplicarDescuento(Producto producto, Long negocioId, DescuentoProductoCommand datos) {
        if (datos == null) {
            // Sin datos de descuento no se toca ninguno: los descuentos del producto (varios, propios o
            // compartidos) se gestionan desde el módulo de descuentos y editar el producto no debe borrarlos.
            return descuentos.buscarActivoPorProducto(producto.getId()).orElse(null);
        }
        return descuentos.buscarPropioActivoDeProducto(producto.getId())
            .map(existente -> {
                existente.modificar(datos.cantidadMinima(), datos.cantidadMaxima(), datos.porcentaje());
                existente.setNombre(nombreDescuento(producto));
                return descuentos.guardar(existente);
            })
            .orElseGet(() -> {
                // El formulario reenvía el descuento que recibió. Si es el heredado de la categoría o el área
                // y no cambió, no se crea una copia propia: seguiría aplicando el original y se desligaría de él.
                Optional<Descuento> heredado = descuentos.buscarActivoPorProducto(producto.getId());
                if (heredado.isPresent() && coincide(heredado.get(), datos)) {
                    return heredado.get();
                }
                return crearDescuento(producto, negocioId, datos);
            });
    }

    private static boolean coincide(Descuento descuento, DescuentoProductoCommand datos) {
        return Objects.equals(descuento.getCantidadMinima(), datos.cantidadMinima())
            && Objects.equals(descuento.getCantidadMaxima(), datos.cantidadMaxima())
            && datos.porcentaje() != null && descuento.getPorcentaje().compareTo(datos.porcentaje()) == 0;
    }

    private static String nombreDescuento(Producto producto) {
        String nombre = "Descuento " + producto.getNombre();
        return nombre.length() <= NOMBRE_DESCUENTO_MAX ? nombre : nombre.substring(0, NOMBRE_DESCUENTO_MAX);
    }
}
