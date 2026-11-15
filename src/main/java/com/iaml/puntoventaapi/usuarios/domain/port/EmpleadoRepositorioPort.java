package com.iaml.puntoventaapi.usuarios.domain.port;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepositorioPort {

    Usuario guardar(Usuario empleado);

    List<Usuario> listarPorNegocio(Long negocioId);

    Optional<Usuario> buscarPorIdYNegocio(Long empleadoId, Long negocioId);

    /** Reemplaza el conjunto de sucursales del empleado. */
    void actualizarSucursales(Long empleadoId, java.util.Collection<Long> sucursalIds);

    void actualizarDatos(Long empleadoId, String nombre, String apellidoPaterno, String apellidoMaterno);

    void eliminar(Long empleadoId);

    boolean existePorCorreo(String correo);
}

