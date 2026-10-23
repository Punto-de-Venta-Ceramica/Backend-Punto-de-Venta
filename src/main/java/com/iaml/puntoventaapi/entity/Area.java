package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "area")
@Getter
@Setter
@Check(name = "ck_area_1", constraints = "btrim(nombre) <> ''")
public class Area extends EntidadConId {

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;
}

