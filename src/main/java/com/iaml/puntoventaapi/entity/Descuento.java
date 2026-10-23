package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "descuento", indexes = {
    @Index(name = "ix_descuento_negocio", columnList = "id_negocio")
})
@Getter
@Setter
@Check(name = "ck_descuento_1", constraints = "btrim(nombre) <> ''")
@Check(name = "ck_descuento_2", constraints = "cantidad_minima > 0")
@Check(name = "ck_descuento_3", constraints = "cantidad_maxima IS NULL OR cantidad_maxima >= cantidad_minima")
@Check(name = "ck_descuento_4", constraints = "descuento BETWEEN 0 AND 100")
public class Descuento extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_negocio", nullable = false)
    private Negocio negocio;

    @NotBlank
    @Size(max = 120)
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @NotNull
    @Min(1)
    @Column(name = "cantidad_minima", nullable = false)
    private Integer cantidadMinima;

    @Min(1)
    @Column(name = "cantidad_maxima")
    private Integer cantidadMaxima;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "descuento", precision = 5, scale = 2, nullable = false)
    private BigDecimal descuento;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "descuento_producto", joinColumns = @JoinColumn(name = "id_descuento"),
        inverseJoinColumns = @JoinColumn(name = "id_producto"),
        indexes = @Index(name = "ix_descuento_producto_producto", columnList = "id_producto"))
    private Set<Producto> productos = new HashSet<>();
}

