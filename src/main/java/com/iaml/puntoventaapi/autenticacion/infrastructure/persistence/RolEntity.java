package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol", check = {
    @CheckConstraint(name = "ck_rol_1", constraint = "btrim(nombre) <> ''")
}, uniqueConstraints = {@UniqueConstraint(name = "uq_rol_nombre", columnNames = "nombre")})
@Getter
@Setter
public class RolEntity extends EntidadConId {

    @NotBlank
    @Size(max = 80)
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "id_rol"),
        inverseJoinColumns = @JoinColumn(name = "id_permiso"),
        indexes = @Index(name = "ix_rol_permiso_permiso", columnList = "id_permiso"))
    private Set<PermisoEntity> permisos = new HashSet<>();
}

