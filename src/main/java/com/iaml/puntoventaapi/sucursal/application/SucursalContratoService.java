package com.iaml.puntoventaapi.sucursal.application;

import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import com.iaml.puntoventaapi.sucursal.domain.port.SucursalRepositorioPort;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SucursalContratoService implements SucursalContrato {

    private final SucursalRepositorioPort sucursales;
    private final SucursalJpaRepository sucursalesJpa;

    public SucursalContratoService(SucursalRepositorioPort sucursales, SucursalJpaRepository sucursalesJpa) {
        this.sucursales = sucursales;
        this.sucursalesJpa = sucursalesJpa;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> idsPorNegocio(Long negocioId, Long usuarioId) {
        return sucursales.buscarPorNegocioId(negocioId, usuarioId).stream().map(Sucursal::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean perteneceANegocio(Long sucursalId, Long negocioId) {
        return sucursalId != null && sucursalId > 0 && negocioId != null && negocioId > 0
            && sucursalesJpa.existsByIdAndNegocio_Id(sucursalId, negocioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> obtenerNombrePorId(Long sucursalId) {
        if (sucursalId == null || sucursalId <= 0) {
            return Optional.empty();
        }
        return sucursalesJpa.findById(sucursalId).map(SucursalEntity::getNombre);
    }
}
