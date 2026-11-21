package com.iaml.puntoventaapi.ventas.application;

import com.iaml.puntoventaapi.ventas.domain.DetalleVenta;
import com.iaml.puntoventaapi.ventas.domain.MetodoPago;
import com.iaml.puntoventaapi.ventas.domain.ProductoSolicitado;
import com.iaml.puntoventaapi.ventas.domain.SolicitudVenta;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.exception.ConflictoConfirmacionVentaException;
import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import com.iaml.puntoventaapi.ventas.domain.port.CajaPort;
import com.iaml.puntoventaapi.ventas.domain.port.InventarioPort;
import com.iaml.puntoventaapi.ventas.domain.port.VentaRepositorioPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmarVentaService {
    private final PrepararVentaService preparacion;
    private final CajaPort caja;
    private final InventarioPort inventario;
    private final VentaRepositorioPort ventas;

    public ConfirmarVentaService(PrepararVentaService preparacion, CajaPort caja,
                                InventarioPort inventario, VentaRepositorioPort ventas) {
        this.preparacion = preparacion;
        this.caja = caja;
        this.inventario = inventario;
        this.ventas = ventas;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VentaConfirmada confirmar(PrepararVentaCommand comando, Long usuarioId, UUID claveOperacion) {

        var solicitud = new SolicitudVenta(comando.sucursalId(), comando.metodoPago(),
            comando.productos(), comando.montoRecibido());
        if (solicitud.metodoPago() == null) {
            throw new DatosVentaInvalidosException(Map.of("metodoPago", "Selecciona EFECTIVO o TARJETA."));
        }
        if (solicitud.metodoPago() == MetodoPago.EFECTIVO && solicitud.montoRecibido() == null) {
            throw new DatosVentaInvalidosException(Map.of("montoRecibido", "Indica el efectivo recibido."));
        }
        Long negocioId = preparacion.resolverNegocio(solicitud.sucursalId(), usuarioId);

        Optional<VentaConfirmada> anterior = buscarReintento(solicitud, usuarioId, claveOperacion);
        if (anterior.isPresent()) {
            return anterior.get();
        }

        var corteBloqueado = caja.bloquearCorteAbierto(solicitud.sucursalId());

        // Otra solicitud con la misma clave pudo confirmarse mientras esperábamos el corte.
        anterior = buscarReintento(solicitud, usuarioId, claveOperacion);
        if (anterior.isPresent()) {
            return anterior.get();
        }

        var corte = corteBloqueado.orElseThrow(() ->
            new ConflictoConfirmacionVentaException("Abre un corte de caja para confirmar la venta."));
        var venta = preparacion.calcular(solicitud, negocioId, usuarioId);
        inventario.descontarParaVenta(solicitud.sucursalId(), solicitud.cantidadesPorProducto());
        return ventas.guardar(venta, corte, claveOperacion);
    }

    private Optional<VentaConfirmada> buscarReintento(SolicitudVenta solicitud, Long usuarioId, UUID claveOperacion) {
        if (claveOperacion == null) {
            return Optional.empty();
        }
        return ventas.buscarPorOperacion(solicitud.sucursalId(), claveOperacion).map(anterior -> {
            var venta = anterior.venta();
            Map<Long, Integer> cantidades = venta.detalles().stream()
                .collect(Collectors.toMap(DetalleVenta::productoId, DetalleVenta::cantidad));
            Set<Long> descuentosSolicitados = solicitud.productos().stream()
                .filter(ProductoSolicitado::aplicarDescuento)
                .map(ProductoSolicitado::productoId)
                .collect(Collectors.toSet());
            Set<Long> descuentosAplicados = venta.detalles().stream()
                .filter(detalle -> detalle.porcentajeDescuento().signum() > 0)
                .map(DetalleVenta::productoId)
                .collect(Collectors.toSet());
            if (!usuarioId.equals(venta.usuarioId()) || solicitud.metodoPago() != venta.metodoPago()
                    || !mismoMonto(solicitud.montoRecibido(), venta.montoRecibido())
                    || !solicitud.cantidadesPorProducto().equals(cantidades)
                    || !descuentosSolicitados.equals(descuentosAplicados)) {
                throw new ConflictoConfirmacionVentaException("La clave de operación ya se usó para otra venta.");
            }
            return anterior;
        });
    }

    /** Se evalúa después de comparar el método: tarjeta no lleva monto y efectivo siempre lo lleva. */
    private static boolean mismoMonto(BigDecimal solicitado, BigDecimal registrado) {
        return solicitado == null || solicitado.compareTo(registrado) == 0;
    }
}
