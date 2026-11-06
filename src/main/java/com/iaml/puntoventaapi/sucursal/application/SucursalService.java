package com.iaml.puntoventaapi.sucursal.application;

import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import com.iaml.puntoventaapi.sucursal.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalNoEncontradaException;
import com.iaml.puntoventaapi.sucursal.domain.port.NegocioPort;
import com.iaml.puntoventaapi.sucursal.domain.port.SucursalRepositorioPort;
import com.iaml.puntoventaapi.sucursal.domain.port.UsuarioAutenticadoPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SucursalService {

    private final SucursalRepositorioPort sucursales;
    private final NegocioPort negocios;
    private final UsuarioAutenticadoPort usuarioAutenticado;

    public SucursalService(SucursalRepositorioPort sucursales, NegocioPort negocios,
                           UsuarioAutenticadoPort usuarioAutenticado) {
        this.sucursales = sucursales;
        this.negocios = negocios;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Transactional
    public Sucursal crear(CrearSucursalCommand command) {
        Long usuarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), usuarioId);
        return sucursales.guardar(new Sucursal(null, command.negocioId(), command.nombre(),
            command.descripcion(), command.ciudad(), command.calle(), command.codigoPostal()), usuarioId);
    }

    @Transactional
    public Sucursal editar(EditarSucursalCommand command) {
        Long usuarioId = usuarioAutenticado.obtenerId();
        Sucursal sucursal = buscarSucursal(command.id(), usuarioId);
        return sucursales.guardar(sucursal.editar(command.nombre(), command.descripcion()), usuarioId);
    }

    @Transactional
    public void eliminar(Long id) {
        Long usuarioId = usuarioAutenticado.obtenerId();
        sucursales.eliminar(id, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Sucursal> consultarPorNegocio(Long negocioId) {
        Long usuarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(negocioId, usuarioId);
        return sucursales.buscarPorNegocioId(negocioId, usuarioId);
    }

    private Sucursal buscarSucursal(Long id, Long usuarioId) {
        return sucursales.buscarPorId(id, usuarioId).orElseThrow(() -> new SucursalNoEncontradaException(id));
    }

    private void verificarAccesoNegocio(Long negocioId, Long usuarioId) {
        // Un negocio ajeno y uno inexistente producen la misma respuesta pública.
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new NegocioNoEncontradoException(negocioId);
        }
    }
}

