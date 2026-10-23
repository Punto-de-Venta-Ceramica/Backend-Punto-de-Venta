package com.iaml.puntoventaapi.negocios.application;

import com.iaml.puntoventaapi.negocios.api.dto.CrearNegocioRequest;
import com.iaml.puntoventaapi.negocios.api.dto.EditarNegocioRequest;
import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;
import com.iaml.puntoventaapi.negocios.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.negocios.infrastructure.imagen.ProcesadorLogo;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.AsociacionUsuarioNegocio;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NegocioService {

    private final NegocioRepository negocios;
    private final AsociacionUsuarioNegocio asociacionUsuarios;
    private final ProcesadorLogo logos;

    @Transactional
    public Negocio crear(CrearNegocioRequest request, Long usuarioId) {
        Negocio negocio = new Negocio();
        negocio.setNombre(request.getNombre().trim());
        negocio.setLogo(logos.procesar(request.getLogo()));
        asociacionUsuarios.asociar(negocio, usuarioId);
        return negocios.save(negocio);
    }

    public List<NegocioResumen> listar(Long usuarioId) {
        return negocios.findAllByUsuarios_IdOrderByNombreAscIdAsc(usuarioId);
    }

    public Negocio consultar(Long negocioId, Long usuarioId) {
        return negocios.findByIdAndUsuarios_Id(negocioId, usuarioId)
            .orElseThrow(NegocioNoEncontradoException::new);
    }

    @Transactional
    public Negocio editar(Long negocioId, EditarNegocioRequest request, Long usuarioId) {
        Negocio negocio = consultar(negocioId, usuarioId);
        String logo = obtenerLogoActualizado(negocio, request);
        negocio.setNombre(request.getNombre().trim());
        negocio.setLogo(logo);
        return negocio;
    }

    private String obtenerLogoActualizado(Negocio negocio, EditarNegocioRequest request) {
        boolean tieneNuevoLogo = request.getLogo() != null && !request.getLogo().isEmpty();
        if (request.isEliminarLogo()) {
            if (tieneNuevoLogo) {
                throw new LogoInvalidoException("No se puede reemplazar y eliminar el logo al mismo tiempo");
            }
            return null;
        }
        return tieneNuevoLogo ? logos.procesar(request.getLogo()) : negocio.getLogo();
    }
}
