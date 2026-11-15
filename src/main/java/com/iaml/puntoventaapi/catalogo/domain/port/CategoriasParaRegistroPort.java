package com.iaml.puntoventaapi.catalogo.domain.port;

import java.util.List;

public interface CategoriasParaRegistroPort {
    record Categoria(long id, String nombre, long areaId, String area) { }
    List<Categoria> listarPorNegocio(long negocioId);
}
