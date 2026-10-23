package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "area", check = {
    @CheckConstraint(name = "ck_area_1", constraint = "btrim(nombre) <> ''")
})
@Getter
@Setter
public class Area extends EntidadConId {

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;
}

