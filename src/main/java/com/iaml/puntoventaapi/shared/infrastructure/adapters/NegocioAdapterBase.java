package com.iaml.puntoventaapi.shared.infrastructure.adapters;

import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Consultas de negocio y sucursal que comparten los adaptadores de los módulos (catálogo, inventario).
 * Cada módulo conserva su propio puerto y solo declara qué operación expone.
 */
public abstract class NegocioAdapterBase {

    private final NegociosContrato negocios;
    private final SucursalContrato sucursales;

    protected NegocioAdapterBase(NegociosContrato negocios, SucursalContrato sucursales) {
        this.negocios = negocios;
        this.sucursales = sucursales;
    }

    @Transactional(readOnly = true)
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return negocios.perteneceAUsuario(negocioId, usuarioId);
    }

    /** Negocio de la sucursal si el usuario (propietario o empleado asignado) puede operarla. */
    @Transactional(readOnly = true)
    protected Optional<Long> negocioAutorizado(Long sucursalId, Long usuarioId) {
        return sucursales.negocioAutorizado(sucursalId, usuarioId);
    }
}
