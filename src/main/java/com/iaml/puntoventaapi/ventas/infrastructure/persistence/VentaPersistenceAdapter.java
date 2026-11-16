package com.iaml.puntoventaapi.ventas.infrastructure.persistence;

import com.iaml.puntoventaapi.caja.domain.Caja;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoEntity;
import com.iaml.puntoventaapi.shared.domain.Folio;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.iaml.puntoventaapi.ventas.domain.VentaConfirmada;
import com.iaml.puntoventaapi.ventas.domain.port.CajaPort.CorteAbierto;
import com.iaml.puntoventaapi.ventas.domain.port.VentaRepositorioPort;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class VentaPersistenceAdapter implements VentaRepositorioPort {
    private final VentaJpaRepository ventas;
    private final DetalleVentaJpaRepository detalles;
    private final EntityManager entityManager;

    public VentaPersistenceAdapter(VentaJpaRepository ventas, DetalleVentaJpaRepository detalles,
                                   EntityManager entityManager) {
        this.ventas = ventas;
        this.detalles = detalles;
        this.entityManager = entityManager;
    }

    @Override
    public VentaConfirmada guardar(com.iaml.puntoventaapi.ventas.domain.Venta venta, CorteAbierto corte,
                                   UUID claveOperacion) {
        Venta entity = new Venta();
        entity.setUsuario(entityManager.getReference(UsuarioEntity.class, venta.usuarioId()));
        entity.setCaja(entityManager.getReference(Caja.class, corte.cajaId()));
        entity.setCorteCaja(entityManager.getReference(CorteCaja.class, corte.corteId()));
        entity.setMetodoPago(venta.metodoPago().name());
        entity.setMontoRecibido(venta.montoRecibido());
        entity.setClaveOperacion(claveOperacion);
        // El UUID solo ocupa el campo obligatorio mientras se conoce el id; el folio visible es corto.
        entity.setFolio(UUID.randomUUID().toString());
        Venta guardada = ventas.save(entity);
        guardada.setFolio(folio(guardada.getId()));
        for (var linea : venta.detalles()) {
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(guardada);
            detalle.setProducto(entityManager.getReference(ProductoEntity.class, linea.productoId()));
            detalle.setNombreProducto(linea.nombreProducto());
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(linea.precioUnitario());
            detalle.setDescAplicado(linea.porcentajeDescuento());
            entityManager.persist(detalle);
        }
        entityManager.flush();
        return new VentaConfirmada(guardada.getId(), guardada.getFolio(), corte.cajaId(), corte.corteId(),
            guardada.getFechaHora(), venta);
    }

    /** Folio que ve el cliente: "VTA-" y el id con al menos 6 dígitos (VTA-000123). */
    static String folio(long ventaId) {
        return Folio.VENTA.de(ventaId);
    }

    @Override
    public Optional<VentaConfirmada> buscarPorOperacion(Long sucursalId, UUID claveOperacion) {
        return ventas.buscarPorOperacion(sucursalId, claveOperacion).map(this::cargarConfirmada);
    }

    @Override
    public Optional<Long> buscarSucursalDeVenta(Long ventaId) {
        return ventas.buscarSucursalDeVenta(ventaId);
    }

    @Override
    public Optional<VentaConfirmada> buscarPorId(Long ventaId) {
        return ventas.findById(ventaId).map(this::cargarConfirmada);
    }

    private VentaConfirmada cargarConfirmada(Venta entity) {
        return VentaMapper.toDomain(entity, detalles.findByVenta_IdOrderById_ProductoIdAsc(entity.getId()));
    }
}
