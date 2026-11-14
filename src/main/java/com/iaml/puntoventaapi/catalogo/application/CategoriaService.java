package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ClasificacionConProductosException;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepositorioPort categorias;
    private final AreaService areas;
    private final ProductoRepositorioPort productos;

    public CategoriaService(CategoriaRepositorioPort categorias, AreaService areas, ProductoRepositorioPort productos) {
        this.categorias = categorias;
        this.areas = areas;
        this.productos = productos;
    }

    @Transactional
    public Categoria crear(Long negocioId, Long areaId, GuardarCategoriaCommand command, Long usuarioId) {
        areas.consultarParaActualizar(negocioId, areaId, usuarioId);
        return categorias.guardar(new Categoria(null, areaId, command.nombre(), command.descripcion()), negocioId);
    }

    public List<Categoria> listar(Long negocioId, Long areaId, Long usuarioId) {
        return listar(negocioId, areaId, usuarioId, null);
    }

    /** Con sucursalId, un empleado asignado a ella también puede consultar (solo lectura). */
    public List<Categoria> listar(Long negocioId, Long areaId, Long usuarioId, Long sucursalId) {
        areas.consultar(negocioId, areaId, usuarioId, sucursalId);
        return categorias.listarPorArea(areaId, negocioId);
    }

    /** Todas las categorías del negocio en una sola consulta, para no pedirlas área por área. */
    public List<Categoria> listarPorNegocio(Long negocioId, Long usuarioId, Long sucursalId) {
        areas.verificarLectura(negocioId, usuarioId, sucursalId);
        return categorias.listarPorNegocio(negocioId);
    }

    public Categoria consultar(Long negocioId, Long areaId, Long categoriaId, Long usuarioId) {
        areas.consultar(negocioId, areaId, usuarioId);
        return buscarCategoria(negocioId, areaId, categoriaId);
    }

    @Transactional
    public Categoria editar(Long negocioId, Long areaId, Long categoriaId,
                            GuardarCategoriaCommand command, Long usuarioId) {
        areas.consultarParaActualizar(negocioId, areaId, usuarioId);
        Categoria categoria = buscarCategoria(negocioId, areaId, categoriaId);
        return categorias.guardar(categoria.editar(command.nombre(), command.descripcion()), negocioId);
    }

    @Transactional
    public void eliminar(Long negocioId, Long areaId, Long categoriaId, Long usuarioId) {
        areas.consultarParaActualizar(negocioId, areaId, usuarioId);
        Categoria categoria = buscarCategoria(negocioId, areaId, categoriaId);
        if (productos.existenPorCategoria(categoriaId)) {
            throw new ClasificacionConProductosException(
                "No se puede eliminar la categoría porque tiene productos relacionados.");
        }
        categorias.guardar(categoria.eliminar(Instant.now()), negocioId);
    }

    private Categoria buscarCategoria(Long negocioId, Long areaId, Long categoriaId) {
        return categorias.buscarPorId(categoriaId, areaId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("La categoría no existe o no tienes acceso."));
    }
}
