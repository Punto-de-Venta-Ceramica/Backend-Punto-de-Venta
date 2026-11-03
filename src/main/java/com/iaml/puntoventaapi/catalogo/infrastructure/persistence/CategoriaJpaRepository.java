package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    @Query("select a.negocio.id from CategoriaEntity c join c.area a where c.id = :categoriaId")
    Optional<Long> buscarNegocioId(@Param("categoriaId") Long categoriaId);
}
