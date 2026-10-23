package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "permiso", uniqueConstraints = {@UniqueConstraint(name = "uq_permiso_nombre", columnNames = "nombre")})
@Getter
@Setter
@Check(name = "ck_permiso_1", constraints = "btrim(nombre) <> ''")
public class Permiso extends EntidadConId {

    @NotBlank
    @Size(max = 120)
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;
}

