package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_venta", check = {
    @CheckConstraint(name = "ck_detalle_venta_cantidad", constraint = "cantidad > 0"),
    @CheckConstraint(name = "ck_detalle_venta_precio", constraint = "precio_unitario >= 0"),
    @CheckConstraint(name = "ck_detalle_venta_descuento", constraint = "desc_aplicado BETWEEN 0 AND 100")
}, indexes = @Index(name = "ix_detalle_venta_producto", columnList = "id_producto"))
@Getter
@Setter
public class DetalleVenta {

    @EmbeddedId
    private DetalleVentaId id = new DetalleVentaId();

    @MapsId("ventaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @MapsId("productoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotNull
    @Min(1)
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "precio_unitario", precision = 14, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "desc_aplicado", precision = 5, scale = 2, nullable = false)
    private BigDecimal descAplicado = new BigDecimal("0");

    @Setter(AccessLevel.NONE)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "subtotal", precision = 18, scale = 2, insertable = false, updatable = false,
        columnDefinition = "numeric(18,2) generated always as (round(cantidad * precio_unitario * (1 - desc_aplicado / 100), 2)) stored")
    private BigDecimal subtotal;
}

