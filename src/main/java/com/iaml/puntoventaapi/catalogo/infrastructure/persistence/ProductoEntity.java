package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;

@Entity
@Table(name = "producto", check = {
    @CheckConstraint(name = "ck_producto_1", constraint = "btrim(nombre) <> ''"),
    @CheckConstraint(name = "ck_producto_precio", constraint = "precio >= 0")
}, indexes = {
    @Index(name = "ix_producto_categoria", columnList = "id_categoria")
})
@Getter
@Setter
public class ProductoEntity extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaEntity categoria;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "precio", precision = 14, scale = 2, nullable = false)
    private BigDecimal precio = BigDecimal.ZERO;

    // Baja lógica: detalle_venta, detalle_pedido e inventario referencian al producto.
    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "imagen_url", columnDefinition = "text")
    private String imagenUrl;

    @Column(name = "imagen_public_id", columnDefinition = "text")
    private String imagenPublicId;
}
