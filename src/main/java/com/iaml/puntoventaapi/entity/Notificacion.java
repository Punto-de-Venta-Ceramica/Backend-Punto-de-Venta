package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import java.time.Instant;

@Entity
@Table(name = "notificacion", indexes = {
    @Index(name = "ix_notificacion_usuario_fecha", columnList = "id_usuario, fecha"),
    @Index(name = "ix_notificacion_venta", columnList = "id_venta"),
    @Index(name = "ix_notificacion_pedido", columnList = "id_pedido")
})
@Getter
@Setter
@Check(name = "ck_notificacion_1", constraints = "btrim(titulo) <> ''")
@Check(name = "ck_notificacion_2", constraints = "(id_venta IS NOT NULL AND id_pedido IS NULL) OR (id_venta IS NULL AND id_pedido IS NOT NULL)")
public class Notificacion extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_venta", nullable = true)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_pedido", nullable = true)
    private Pedido pedido;

    @NotBlank
    @Size(max = 200)
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @NotNull
    @Column(name = "mensaje", nullable = false, columnDefinition = "text")
    private String mensaje;

    @CurrentTimestamp(event = org.hibernate.generator.EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha", nullable = false, columnDefinition = "timestamptz")
    private Instant fecha;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "leida", nullable = false)
    private Boolean leida = false;
}

