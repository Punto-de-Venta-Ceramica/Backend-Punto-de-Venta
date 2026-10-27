package com.iaml.puntoventaapi.inventario.domain;

import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "inventario", check = {
    @CheckConstraint(name = "ck_inventario_1", constraint = "existencia >= 0")
}, uniqueConstraints = {@UniqueConstraint(name = "uq_inventario_sucursal_producto", columnNames = {"id_sucursal", "id_producto"})}, indexes = {
    @Index(name = "ix_inventario_producto", columnList = "id_producto")
})
@Getter
@Setter
public class Inventario extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private SucursalEntity sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotNull
    @Min(0)
    @ColumnDefault("0")
    @Column(name = "existencia", nullable = false)
    private Integer existencia = 0;
}

