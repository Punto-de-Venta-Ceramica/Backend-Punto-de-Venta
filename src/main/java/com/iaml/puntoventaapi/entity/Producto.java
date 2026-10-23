package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "producto", indexes = {
    @Index(name = "ix_producto_categoria", columnList = "id_categoria")
})
@Getter
@Setter
@Check(name = "ck_producto_1", constraints = "btrim(nombre) <> ''")
public class Producto extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;
}

