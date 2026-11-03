package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoriaPersistenceAdapter implements CategoriaRepositorioPort {

    private final CategoriaJpaRepository categorias;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarNegocioId(Long categoriaId) {
        return categorias.buscarNegocioId(categoriaId);
    }
}
