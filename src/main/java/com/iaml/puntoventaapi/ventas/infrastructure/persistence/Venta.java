package com.iaml.puntoventaapi.ventas.infrastructure.persistence;

import com.iaml.puntoventaapi.caja.domain.Caja;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venta", uniqueConstraints = {
    @UniqueConstraint(name = "uq_venta_caja_operacion", columnNames = {"id_caja", "clave_operacion"}),
    // Cada sucursal tiene una sola caja: folio único por caja = folio único por sucursal (como en pedido).
    @UniqueConstraint(name = "uq_venta_folio_caja", columnNames = {"id_caja", "folio"})
}, check = {
    @CheckConstraint(name = "ck_venta_1", constraint = "btrim(metodo_pago) <> ''"),
    @CheckConstraint(name = "ck_venta_2", constraint = "btrim(folio) <> ''")
}, indexes = {
    @Index(name = "ix_venta_usuario", columnList = "id_usuario"),
    @Index(name = "ix_venta_caja_fecha", columnList = "id_caja, fecha_hora"),
    @Index(name = "ix_venta_corte", columnList = "id_corte_caja, id_caja")
})
@Getter
@Setter
public class Venta extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_corte_caja", nullable = false)
    private CorteCaja corteCaja;

    @NotBlank
    @Size(max = 50)
    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @CurrentTimestamp(event = org.hibernate.generator.EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaHora;

    @NotBlank
    @Size(max = 50)
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @Column(name = "monto_recibido", precision = 18, scale = 2, updatable = false)
    private BigDecimal montoRecibido;

    @Column(name = "clave_operacion", updatable = false)
    private UUID claveOperacion;
}

