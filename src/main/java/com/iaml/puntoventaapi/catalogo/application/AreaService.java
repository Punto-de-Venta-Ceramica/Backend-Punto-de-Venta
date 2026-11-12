package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ClasificacionConProductosException;
import com.iaml.puntoventaapi.catalogo.domain.port.AreaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepositorioPort areas;
    private final NegociosContrato negocios;
    private final CategoriaRepositorioPort categorias;
    private final ProductoRepositorioPort productos;
    private final SucursalContrato sucursales;

    public AreaService(AreaRepositorioPort areas, NegociosContrato negocios,
                       CategoriaRepositorioPort categorias, ProductoRepositorioPort productos,
                       SucursalContrato sucursales) {
        this.sucursales = sucursales;
        this.areas = areas;
        this.negocios = negocios;
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional
    public Area crear(Long negocioId, GuardarAreaCommand command, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return areas.guardar(new Area(null, negocioId, command.nombre(), command.descripcion()));
    }

    public List<Area> listar(Long negocioId, Long usuarioId) {
        return listar(negocioId, usuarioId, null);
    }

    /** Con sucursalId, un empleado asignado a ella también puede consultar (solo lectura). */
    public List<Area> listar(Long negocioId, Long usuarioId, Long sucursalId) {
        verificarLectura(negocioId, usuarioId, sucursalId);
        return areas.listarPorNegocio(negocioId);
    }

    public Area consultar(Long negocioId, Long areaId, Long usuarioId, Long sucursalId) {
        verificarLectura(negocioId, usuarioId, sucursalId);
        return areas.buscarPorId(areaId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("El área no existe o no tienes acceso."));
    }

    public Area consultar(Long negocioId, Long areaId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return areas.buscarPorId(areaId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("El área no existe o no tienes acceso."));
    }

    @Transactional
    public Area editar(Long negocioId, Long areaId, GuardarAreaCommand command, Long usuarioId) {
        Area area = consultarParaActualizar(negocioId, areaId, usuarioId);
        return areas.guardar(area.editar(command.nombre(), command.descripcion()));
    }

    @Transactional
    public void eliminar(Long negocioId, Long areaId, Long usuarioId) {
        Area area = consultarParaActualizar(negocioId, areaId, usuarioId);
        if (productos.existenPorArea(areaId)) {
            throw new ClasificacionConProductosException(
                "No se puede eliminar el área porque alguna de sus categorías tiene productos relacionados.");
        }
        Instant fecha = Instant.now();
        for (Categoria categoria : categorias.listarPorArea(areaId, negocioId)) {
            categorias.guardar(categoria.eliminar(fecha), negocioId);
        }
        areas.guardar(area.eliminar(fecha));
    }

    @Transactional
    public Area consultarParaActualizar(Long negocioId, Long areaId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return areas.buscarPorIdParaActualizar(areaId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("El área no existe o no tienes acceso."));
    }

    private void verificarLectura(Long negocioId, Long usuarioId, Long sucursalId) {
        boolean empleado = sucursalId != null && sucursales.negocioAutorizado(sucursalId, usuarioId)
            .filter(negocioId::equals).isPresent();
        if (!empleado) {
            verificarNegocio(negocioId, usuarioId);
        }
    }

    private void verificarNegocio(Long negocioId, Long usuarioId) {
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new CatalogoNoEncontradoException("El negocio no existe o no tienes acceso.");
        }
    }
}
