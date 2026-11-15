package com.iaml.puntoventaapi.inventario.domain.port;

public interface NegocioPort {

    boolean perteneceAUsuario(Long negocioId, Long usuarioId);

    /**
     * El propietario del negocio, o un empleado asignado a esa sucursal, puede operar el inventario de la
     * sucursal. Un empleado nunca accede a las existencias de sus otras sucursales.
     */
    boolean puedeOperarSucursal(Long negocioId, Long sucursalId, Long usuarioId);
}
