package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import com.iaml.puntoventaapi.clientes.domain.Cliente;
import com.iaml.puntoventaapi.negocios.domain.Sucursal;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import java.time.Instant;

@Entity
@Table(name = "pedido", check = {
    @CheckConstraint(name = "ck_pedido_1", constraint = "btrim(folio) <> ''"),
    @CheckConstraint(name = "ck_pedido_2", constraint = "btrim(tipo_entrega) <> ''"),
    @CheckConstraint(name = "ck_pedido_3", constraint = "btrim(estado) <> ''"),
    @CheckConstraint(name = "ck_pedido_4", constraint = "fecha_entrega IS NULL OR fecha_entrega >= fecha_creacion")
}, uniqueConstraints = {@UniqueConstraint(name = "uq_pedido_folio_sucursal", columnNames = {"id_sucursal", "folio"})}, indexes = {
    @Index(name = "ix_pedido_usuario", columnList = "id_usuario"),
    @Index(name = "ix_pedido_cliente", columnList = "id_cliente"),
    @Index(name = "ix_pedido_sucursal_fecha", columnList = "id_sucursal, fecha_creacion")
})
@Getter
@Setter
public class Pedido extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @NotBlank
    @Size(max = 50)
    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @NotBlank
    @Size(max = 50)
    @Column(name = "tipo_entrega", nullable = false, length = 50)
    private String tipoEntrega;

    @NotBlank
    @Size(max = 50)
    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "descripcion_envio", columnDefinition = "text")
    private String descripcionEnvio;

    @CurrentTimestamp(event = org.hibernate.generator.EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_creacion", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaCreacion;

    @Column(name = "fecha_entrega", columnDefinition = "timestamptz")
    private Instant fechaEntrega;
}

