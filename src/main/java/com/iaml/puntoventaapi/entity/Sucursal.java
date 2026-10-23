package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sucursal", indexes = {
    @Index(name = "ix_sucursal_negocio", columnList = "id_negocio")
})
@Getter
@Setter
@Check(name = "ck_sucursal_1", constraints = "btrim(nombre) <> ''")
public class Sucursal extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_negocio", nullable = false)
    private Negocio negocio;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Size(max = 120)
    @Column(name = "ciudad", length = 120)
    private String ciudad;

    @Size(max = 200)
    @Column(name = "calle", length = 200)
    private String calle;

    @Size(max = 10)
    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sucursal_area", joinColumns = @JoinColumn(name = "id_sucursal"),
        inverseJoinColumns = @JoinColumn(name = "id_area"),
        indexes = @Index(name = "ix_sucursal_area_area", columnList = "id_area"))
    private Set<Area> areas = new HashSet<>();
}

