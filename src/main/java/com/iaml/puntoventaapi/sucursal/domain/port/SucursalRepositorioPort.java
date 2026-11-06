package com.iaml.puntoventaapi.sucursal.domain.port;

import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import java.util.Optional;
import java.util.List;

public interface SucursalRepositorioPort {

    Sucursal guardar(Sucursal sucursal, Long usuarioId);

    Optional<Sucursal> buscarPorId(Long id, Long usuarioId);

    List<Sucursal> buscarPorNegocioId(Long negocioId, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}

