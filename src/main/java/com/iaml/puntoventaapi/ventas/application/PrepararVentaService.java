package com.iaml.puntoventaapi.ventas.application;

import com.iaml.puntoventaapi.ventas.domain.DetalleVenta;
import com.iaml.puntoventaapi.ventas.domain.ProductoCotizado;
import com.iaml.puntoventaapi.ventas.domain.ProductoDisponible;
import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import com.iaml.puntoventaapi.ventas.domain.SolicitudVenta;
import com.iaml.puntoventaapi.ventas.domain.Venta;
import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import com.iaml.puntoventaapi.ventas.domain.exception.ExistenciaVentaInsuficienteException;
import com.iaml.puntoventaapi.ventas.domain.exception.RecursoVentaNoEncontradoException;
import com.iaml.puntoventaapi.ventas.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.ventas.domain.port.InventarioPort;
import com.iaml.puntoventaapi.ventas.domain.port.SucursalPort;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class PrepararVentaService {

    private final SucursalPort sucursales;
    private final CatalogoPort catalogo;
    private final InventarioPort inventario;

    public PrepararVentaService(SucursalPort sucursales, CatalogoPort catalogo, InventarioPort inventario) {
        this.sucursales = sucursales;
        this.catalogo = catalogo;
        this.inventario = inventario;
    }

    /** Solo consulta: la confirmación deberá volver a validar precios y existencias antes de escribir. */
    public Venta preparar(PrepararVentaCommand command, Long usuarioId) {
        SolicitudVenta solicitud = new SolicitudVenta(
            command.sucursalId(), command.metodoPago(), command.productos(), command.montoRecibido());
        Long negocioId = resolverNegocio(solicitud.sucursalId(), usuarioId);
        Map<Long, Integer> existencias = inventario.consultarExistencias(negocioId, solicitud.sucursalId(), usuarioId);
        Map<Long, ProductoCotizado> productos = catalogo.cotizarProductos(negocioId, solicitud.cantidadesPorProducto());
        List<DetalleVenta> detalles = solicitud.productos().stream()
            .map(seleccion -> prepararDetalle(seleccion, productos, existencias)).toList();
        return new Venta(usuarioId, negocioId, solicitud.sucursalId(), solicitud.metodoPago(),
            detalles, solicitud.montoRecibido());
    }

    /** Incluye existencia cero para que el cliente pueda mostrar los productos agotados. */
    public List<ProductoDisponible> listarProductos(Long sucursalId, Long usuarioId) {
        Long negocioId = resolverNegocio(sucursalId, usuarioId);
        Map<Long, Integer> existencias = inventario.consultarExistencias(negocioId, sucursalId, usuarioId);
        Map<Long, Integer> cantidades = existencias.keySet().stream()
            .collect(Collectors.toUnmodifiableMap(id -> id, id -> 1));
        return catalogo.cotizarProductos(negocioId, cantidades).values().stream()
            .map(p -> new ProductoDisponible(p.productoId(), p.nombre(), p.precioUnitario(),
                existencias.get(p.productoId())))
            .sorted(Comparator.comparing(ProductoDisponible::nombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProductoDisponible::productoId))
            .toList();
    }

    private Long resolverNegocio(Long sucursalId, Long usuarioId) {
        if (sucursalId == null || sucursalId <= 0) {
            throw new DatosVentaInvalidosException(Map.of("sucursalId", "Selecciona una sucursal válida."));
        }
        return sucursales.obtenerNegocioIdAccesible(sucursalId, usuarioId)
            .orElseThrow(() -> new RecursoVentaNoEncontradoException("La sucursal no existe o no tienes acceso."));
    }

    private DetalleVenta prepararDetalle(ProductoSolicitado seleccion, Map<Long, ProductoCotizado> productos,
                                         Map<Long, Integer> existencias) {
        ProductoCotizado producto = productos.get(seleccion.productoId());
        Integer existencia = existencias.get(seleccion.productoId());
        if (producto == null || existencia == null) {
            throw new RecursoVentaNoEncontradoException(
                "Algún producto no está disponible en el catálogo de esta sucursal.");
        }
        if (seleccion.cantidad() > existencia) {
            throw new ExistenciaVentaInsuficienteException(seleccion.productoId(), seleccion.cantidad(), existencia);
        }
        return new DetalleVenta(producto.productoId(), producto.nombre(), seleccion.cantidad(),
            producto.precioUnitario(), producto.porcentajeDescuento());
    }
}
