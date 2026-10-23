package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "categoria", check = {
    @CheckConstraint(name = "ck_categoria_1", constraint = "btrim(nombre) <> ''")
}, indexes = {
    @Index(name = "ix_categoria_area", columnList = "id_area")
})
@Getter
@Setter
public class Categoria extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_area", nullable = false)
    private Area area;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;
}

