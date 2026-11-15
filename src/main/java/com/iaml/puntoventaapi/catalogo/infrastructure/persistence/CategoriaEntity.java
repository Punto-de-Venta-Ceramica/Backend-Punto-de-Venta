package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria", check = {
    @CheckConstraint(name = "ck_categoria_1", constraint = "btrim(nombre) <> ''")
}, indexes = {
    @Index(name = "ix_categoria_area", columnList = "id_area")
})
@Getter
@Setter
public class CategoriaEntity extends ClasificacionEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_area", nullable = false)
    private AreaEntity area;
}
