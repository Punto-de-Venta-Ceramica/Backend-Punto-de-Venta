package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "permiso", check = {
    @CheckConstraint(name = "ck_permiso_1", constraint = "btrim(nombre) <> ''")
}, uniqueConstraints = {@UniqueConstraint(name = "uq_permiso_nombre", columnNames = "nombre")})
@Getter
@Setter
public class PermisoEntity extends EntidadConId {

    @NotBlank
    @Size(max = 120)
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;
}

