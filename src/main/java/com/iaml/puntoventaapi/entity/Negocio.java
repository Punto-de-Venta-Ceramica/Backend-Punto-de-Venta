package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "negocio")
@Getter
@Setter
@Check(name = "ck_negocio_1", constraints = "btrim(nombre) <> ''")
public class Negocio extends EntidadConId {

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "logo", columnDefinition = "text")
    private String logo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "negocio_usuario", joinColumns = @JoinColumn(name = "id_negocio"),
        inverseJoinColumns = @JoinColumn(name = "id_usuario"),
        indexes = @Index(name = "ix_negocio_usuario_usuario", columnList = "id_usuario"))
    private Set<Usuario> usuarios = new HashSet<>();
}

