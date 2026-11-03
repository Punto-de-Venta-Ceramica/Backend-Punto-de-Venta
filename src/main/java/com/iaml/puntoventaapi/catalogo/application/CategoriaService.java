package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepositorioPort categorias;
    private final AreaService areas;

    public CategoriaService(CategoriaRepositorioPort categorias, AreaService areas) {
        this.categorias = categorias;
        this.areas = areas;
    }

    @Transactional
    public Categoria crear(Long negocioId, Long areaId, GuardarCategoriaCommand command, Long usuarioId) {
        areas.consultar(negocioId, areaId, usuarioId);
        return categorias.guardar(new Categoria(null, areaId, command.nombre(), command.descripcion()), negocioId);
    }

    public List<Categoria> listar(Long negocioId, Long areaId, Long usuarioId) {
        areas.consultar(negocioId, areaId, usuarioId);
        return categorias.listarPorArea(areaId, negocioId);
    }

    public Categoria consultar(Long negocioId, Long areaId, Long categoriaId, Long usuarioId) {
        areas.consultar(negocioId, areaId, usuarioId);
        return categorias.buscarPorId(categoriaId, areaId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("La categoría no existe o no tienes acceso."));
    }

    @Transactional
    public Categoria editar(Long negocioId, Long areaId, Long categoriaId,
                            GuardarCategoriaCommand command, Long usuarioId) {
        Categoria categoria = consultar(negocioId, areaId, categoriaId, usuarioId);
        return categorias.guardar(categoria.editar(command.nombre(), command.descripcion()), negocioId);
    }
}
