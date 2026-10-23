package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol", uniqueConstraints = {@UniqueConstraint(name = "uq_rol_nombre", columnNames = "nombre")})
@Getter
@Setter
@Check(name = "ck_rol_1", constraints = "btrim(nombre) <> ''")
public class Rol extends EntidadConId {

    @NotBlank
    @Size(max = 80)
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "id_rol"),
        inverseJoinColumns = @JoinColumn(name = "id_permiso"),
        indexes = @Index(name = "ix_rol_permiso_permiso", columnList = "id_permiso"))
    private Set<Permiso> permisos = new HashSet<>();
}

