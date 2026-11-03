package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.AreaRepositorioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepositorioPort areas;
    private final NegociosContrato negocios;

    public AreaService(AreaRepositorioPort areas, NegociosContrato negocios) {
        this.areas = areas;
        this.negocios = negocios;
    }

    @Transactional
    public Area crear(Long negocioId, GuardarAreaCommand command, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return areas.guardar(new Area(null, negocioId, command.nombre(), command.descripcion()));
    }

    public List<Area> listar(Long negocioId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return areas.listarPorNegocio(negocioId);
    }

    public Area consultar(Long negocioId, Long areaId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return areas.buscarPorId(areaId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("El área no existe o no tienes acceso."));
    }

    @Transactional
    public Area editar(Long negocioId, Long areaId, GuardarAreaCommand command, Long usuarioId) {
        Area area = consultar(negocioId, areaId, usuarioId);
        return areas.guardar(area.editar(command.nombre(), command.descripcion()));
    }

    private void verificarNegocio(Long negocioId, Long usuarioId) {
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new CatalogoNoEncontradoException("El negocio no existe o no tienes acceso.");
        }
    }
}
