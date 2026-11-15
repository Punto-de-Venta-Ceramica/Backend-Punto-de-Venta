package com.iaml.puntoventaapi.inventario.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoEntity;
import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventarioPersistenceAdapter implements InventarioRepositorioPort {

    private final InventarioJpaRepository inventarios;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Inventario guardar(Inventario inventario) {
        InventarioEntity entity;
        if (inventario.getId() == null) {
            SucursalEntity sucursal = entityManager.getReference(SucursalEntity.class, inventario.getSucursalId());
            ProductoEntity producto = entityManager.getReference(ProductoEntity.class, inventario.getProductoId());
            entity = InventarioMapper.toEntity(inventario, sucursal, producto);
        } else {
            entity = inventarios.findById(inventario.getId())
                .orElseThrow(() -> new InventarioNoEncontradoException());
            entity.setExistencia(inventario.getExistencia());
            entity.setActivo(inventario.isActivo());
        }
        return InventarioMapper.toDomain(inventarios.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Optional<Inventario> buscarParaAjuste(Long inventarioId, Long usuarioId) {
        if (!inventarios.esAccesibleParaUsuario(inventarioId, usuarioId)) {
            return Optional.empty();
        }
        return inventarios.buscarConBloqueo(inventarioId).map(InventarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> negocioDeProducto(Long productoId, Long usuarioId) {
        return inventarios.negocioDeProducto(productoId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> negocioDeSucursal(Long sucursalId, Long usuarioId) {
        return inventarios.negocioDeSucursal(sucursalId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Inventario> buscarPorSucursalYProducto(Long sucursalId, Long productoId) {
        return inventarios.findBySucursalIdAndProductoId(sucursalId, productoId).map(InventarioMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Inventario> buscarParaQuitar(Long inventarioId, Long usuarioId) {
        if (!inventarios.esDelPropietario(inventarioId, usuarioId)) {
            return Optional.empty();
        }
        return inventarios.buscarConBloqueo(inventarioId).map(InventarioMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarActivosDeProducto(Long productoId) {
        return inventarios.contarActivosDeProducto(productoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExistenciaProducto> listarPanel(Long negocioId, Long sucursalId, Long productoId, Long usuarioId) {
        List<Object[]> filas = sucursalId == null
            ? inventarios.panelPorNegocio(negocioId, productoId, usuarioId)
            : inventarios.panelPorSucursal(negocioId, sucursalId, productoId, usuarioId);
        return filas.stream().map(InventarioMapper::toExistencia).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExistenciaProducto> listarPorProducto(Long productoId, Long usuarioId) {
        return inventarios.existenciasDeProducto(productoId, usuarioId).stream()
            .map(InventarioMapper::toExistencia).toList();
    }
}
