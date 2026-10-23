package com.iaml.puntoventaapi.notificaciones.domain;

import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.Pedido;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.iaml.puntoventaapi.ventas.infrastructure.persistence.Venta;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import java.time.Instant;

@Entity
@Table(name = "notificacion", check = {
    @CheckConstraint(name = "ck_notificacion_1", constraint = "btrim(titulo) <> ''"),
    @CheckConstraint(name = "ck_notificacion_2", constraint = "(id_venta IS NOT NULL AND id_pedido IS NULL) OR (id_venta IS NULL AND id_pedido IS NOT NULL)")
}, indexes = {
    @Index(name = "ix_notificacion_usuario_fecha", columnList = "id_usuario, fecha"),
    @Index(name = "ix_notificacion_venta", columnList = "id_venta"),
    @Index(name = "ix_notificacion_pedido", columnList = "id_pedido")
})
@Getter
@Setter
public class Notificacion extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

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

