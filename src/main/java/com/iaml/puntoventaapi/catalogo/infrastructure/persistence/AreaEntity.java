package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "area", check = {
    @CheckConstraint(name = "ck_area_1", constraint = "btrim(nombre) <> ''")
}, indexes = {
    @Index(name = "ix_area_negocio", columnList = "id_negocio")
})
@Getter
@Setter
public class AreaEntity extends ClasificacionEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_negocio", nullable = false, foreignKey = @ForeignKey(name = "fk_area_negocio"))
    private NegocioEntity negocio;
}
