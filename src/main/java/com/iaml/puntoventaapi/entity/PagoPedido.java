package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pago_pedido", indexes = {
    @Index(name = "ix_pago_pedido_pedido", columnList = "id_pedido"),
    @Index(name = "ix_pago_pedido_corte", columnList = "id_corte_caja")
})
@Getter
@Setter
@Check(name = "ck_pago_pedido_1", constraints = "btrim(metodo_pago) <> ''")
@Check(name = "ck_pago_pedido_2", constraints = "btrim(tipo_pago) <> ''")
@Check(name = "ck_pago_pedido_3", constraints = "monto > 0")
public class PagoPedido extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_corte_caja", nullable = false)
    private CorteCaja corteCaja;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2)
    @Column(name = "monto", precision = 18, scale = 2, nullable = false)
    private BigDecimal monto;

    @NotBlank
    @Size(max = 50)
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @CurrentTimestamp(event = org.hibernate.generator.EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaHora;

    @NotBlank
    @Size(max = 50)
    @Column(name = "tipo_pago", nullable = false, length = 50)
    private String tipoPago;
}

