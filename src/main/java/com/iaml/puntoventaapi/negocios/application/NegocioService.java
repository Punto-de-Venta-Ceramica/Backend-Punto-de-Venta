package com.iaml.puntoventaapi.negocios.application;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;
import com.iaml.puntoventaapi.negocios.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.negocios.domain.port.NegocioRepositorioPort;
import com.iaml.puntoventaapi.negocios.domain.port.ProcesadorLogoPort;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import com.iaml.puntoventaapi.usuarios.UsuariosContrato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NegocioService {

    private final NegocioRepositorioPort negocios;
    private final UsuariosContrato usuarios;
    private final ProcesadorLogoPort logos;

    public NegocioService(NegocioRepositorioPort negocios, UsuariosContrato usuarios, ProcesadorLogoPort logos) {
        this.negocios = negocios;
        this.usuarios = usuarios;
        this.logos = logos;
    }

    @Transactional
    public Negocio crear(CrearNegocioCommand command, Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0 || !usuarios.existePorId(usuarioId)) {
            throw new UsuarioNoAutenticadoException();
        }
        Negocio negocio = Negocio.crear(command.nombre(), logos.procesar(command.logo()));
        return negocios.guardar(negocio, usuarioId);
    }

    public List<NegocioResumen> listar(Long usuarioId) {
        return negocios.listarPorUsuario(usuarioId);
    }

    public Negocio consultar(Long negocioId, Long usuarioId) {
        return negocios.buscarPorId(negocioId, usuarioId)
            .orElseThrow(NegocioNoEncontradoException::new);
    }

    @Transactional
    public Negocio editar(Long negocioId, EditarNegocioCommand command, Long usuarioId) {
        Negocio negocio = consultar(negocioId, usuarioId);
        String logo = obtenerLogoActualizado(negocio, command);
        return negocios.guardar(negocio.editar(command.nombre(), logo), usuarioId);
    }

    @Transactional
    public void eliminar(Long negocioId, Long usuarioId) {
        Negocio negocio = consultar(negocioId, usuarioId);
        negocios.guardar(negocio.eliminar(), usuarioId);
    }

    private String obtenerLogoActualizado(Negocio negocio, EditarNegocioCommand command) {
        boolean tieneNuevoLogo = command.logo() != null;
        if (command.eliminarLogo()) {
            if (tieneNuevoLogo) {
                throw new LogoInvalidoException("No se puede reemplazar y eliminar el logo al mismo tiempo");
            }
            return null;
        }
        return tieneNuevoLogo ? logos.procesar(command.logo()) : negocio.getLogo();
    }
}
