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
import java.time.Instant;

@Entity
@Table(name = "venta", check = {
    @CheckConstraint(name = "ck_venta_1", constraint = "btrim(metodo_pago) <> ''")
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

    @CurrentTimestamp(event = org.hibernate.generator.EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaHora;

    @NotBlank
    @Size(max = 50)
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;
}

