package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.AlmacenImagenesPort;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.InventarioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.SucursalPort;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(ProductoService.class);
    private static final int NOMBRE_DESCUENTO_MAX = 120;
    public static final int TAMANIO_PAGINA_MAX = 100;

    private final ProductoRepositorioPort productos;
    private final CategoriaRepositorioPort categorias;
    private final DescuentoRepositorioPort descuentos;
    private final NegocioPort negocios;
    private final SucursalPort sucursales;
    private final InventarioPort inventario;
    private final AlmacenImagenesPort almacenImagenes;

    /** Producto, descuento (opcional) e inventario por sucursal se guardan en una sola transacción. */
    @Transactional
    public ProductoDetalle crear(CrearProductoCommand command, Long usuarioId) {
        Long negocioId = resolverNegocioDeCategoria(command.categoriaId(), usuarioId);
        Map<Long, Integer> existencias = resolverExistencias(negocioId, command.existencias(), usuarioId);

        Producto producto = productos.guardar(new Producto(null, command.categoriaId(), command.nombre(),
            command.precio(), command.imagenUrl(), command.imagenPublicId(), true));
        // Sin descuento propio, el producto puede heredar el de su categoría o área.
        Descuento descuento = command.descuento() == null
            ? descuentos.buscarActivoPorProducto(producto.getId()).orElse(null)
            : crearDescuento(producto, negocioId, command.descuento());
        inventario.registrarExistenciasIniciales(producto.getId(), existencias);
        return conClasificacion(producto, descuento);
    }

    /** Nunca modifica la existencia: eso es responsabilidad del módulo de inventario. */
    @Transactional
    public ProductoDetalle editar(EditarProductoCommand command, Long usuarioId) {
        Producto producto = buscarProducto(command.id(), usuarioId);
        Long categoriaAnterior = producto.getCategoriaId();
        String publicIdAnterior = producto.getImagenPublicId();
        String urlAnterior = producto.getImagenUrl();
        Long negocioId = categorias.buscarNegocioId(categoriaAnterior)
            .orElseThrow(() -> new CategoriaNoEncontradaException());

        producto.setNombre(command.nombre());
        producto.setPrecio(command.precio());
        producto.setCategoriaId(command.categoriaId());
        producto.setImagenUrl(command.imagenUrl());
        producto.setImagenPublicId(command.imagenPublicId());
        Long negocioNuevo = resolverNegocioDeCategoria(producto.getCategoriaId(), usuarioId);
        if (!negocioNuevo.equals(negocioId)) {
            throw new ProductoInvalidoException("La categoría debe pertenecer al mismo negocio del producto.");
        }

        Producto guardado = productos.guardar(producto);
        Descuento descuento = aplicarDescuento(guardado, negocioId, command.descuento());
        if (cambioImagen(publicIdAnterior, urlAnterior, guardado) && publicIdAnterior != null) {
            eliminarImagenSegura(publicIdAnterior, guardado.getId());
        }
        return conClasificacion(guardado, descuento);
    }

    /** Baja lógica: el producto sigue referenciado por ventas, pedidos e inventario. */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Producto producto = buscarProducto(id, usuarioId);
        String publicIdAnterior = producto.getImagenPublicId();
        producto.desactivar();
        productos.guardar(producto);
        if (publicIdAnterior != null) {
            eliminarImagenSegura(publicIdAnterior, id);
        }
    }

    /**
     * El identificador de la imagen lo envía el cliente: si otro producto la usa, no es (solo) de este y no
     * se borra. Así nadie puede eliminar la imagen de un producto ajeno apuntando el suyo a ella.
     */
    private void eliminarImagenSegura(String publicId, Long productoId) {
        if (publicId == null || publicId.isBlank() || productos.imagenUsadaPorOtroProducto(publicId, productoId)) {
            return;
        }
        try {
            almacenImagenes.eliminar(publicId);
        } catch (Exception ex) {
            LOG.error("No se pudo eliminar la imagen '{}' en Cloudinary; la operación del producto continúa.", publicId, ex);
        }
    }

    private static boolean cambioImagen(String publicIdAnterior, String urlAnterior, Producto actual) {
        return !Objects.equals(publicIdAnterior, actual.getImagenPublicId())
            || !Objects.equals(urlAnterior, actual.getImagenUrl());
    }

    @Transactional(readOnly = true)
    public ProductoDetalle consultar(Long id, Long usuarioId) {
        return consultar(id, usuarioId, null);
    }

    /** Con sucursalId, un empleado asignado a ella también puede consultar productos de ese negocio. */
    @Transactional(readOnly = true)
    public ProductoDetalle consultar(Long id, Long usuarioId, Long sucursalId) {
        ProductoDetalle detalle = productos.buscarConClasificacion(id, usuarioId)
            .or(() -> sucursalId == null ? Optional.empty()
                : negocios.negocioDeSucursal(sucursalId, usuarioId)
                    .flatMap(negocioId -> productos.buscarConClasificacionEnNegocio(id, negocioId)))
            .orElseThrow(() -> new ProductoNoEncontradoException());
        // Una sola búsqueda: los escalones del nivel que aplica (producto > categoría > área).
        return ProductoDetalle.conEscalones(detalle.producto(), detalle.clasificacion(),
            descuentos.buscarEscalonesActivosPorProducto(id));
    }

    /**
     * Página de productos para el listado: solo el producto (imagen, nombre, precio). Descuento, existencia,
     * área y categoría se consultan en el detalle.
     */
    @Transactional(readOnly = true)
    public Pagina<Producto> listar(Long negocioId, Long usuarioId, FiltroProductos filtro,
                                   int pagina, int tamanio) {
        // El empleado solo lista productos de una sucursal suya (filtro obligatorio).
        if (!negocios.puedeConsultar(negocioId, filtro.sucursalId(), usuarioId)) {
            throw new NegocioNoEncontradoException();
        }
        return productos.listarPagina(negocioId, filtro,
            Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), TAMANIO_PAGINA_MAX));
    }

    /** Todos los productos activos del negocio, sin existencia ni descuento: lo usan los selectores de otros módulos. */
    @Transactional(readOnly = true)
    public List<Producto> catalogo(Long negocioId, Long usuarioId) {
        validarNegocio(negocioId, usuarioId);
        return productos.listarPorNegocio(negocioId);
    }

    /** Un negocio ajeno y uno inexistente producen la misma respuesta pública. */
    private void validarNegocio(Long negocioId, Long usuarioId) {
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new NegocioNoEncontradoException();
        }
    }

    private ProductoDetalle conClasificacion(Producto producto, Descuento descuento) {
        ClasificacionProducto clasificacion = productos.buscarClasificacion(producto.getCategoriaId()).orElse(null);
        // El descuento recién guardado se conserva; los escalones son los que realmente aplican.
        List<Descuento> escalones = descuentos.buscarEscalonesActivosPorProducto(producto.getId());
        return new ProductoDetalle(producto, descuento, clasificacion, escalones);
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
