package com.iaml.puntoventaapi.caja.domain;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "corte_caja", check = {
    @CheckConstraint(name = "ck_corte_caja_1", constraint = "monto_inicial >= 0 AND total_efectivo >= 0 AND total_tarjeta >= 0 AND total_estimado >= 0"),
    @CheckConstraint(name = "ck_corte_caja_2", constraint = "total_real IS NULL OR total_real >= 0"),
    @CheckConstraint(name = "ck_corte_caja_3", constraint = "fecha_hora_cierre IS NULL OR fecha_hora_cierre >= fecha_hora_apertura"),
    @CheckConstraint(name = "ck_corte_caja_4", constraint = "fecha_hora_cierre IS NULL OR total_real IS NOT NULL")
}, uniqueConstraints = {@UniqueConstraint(name = "uq_corte_caja_id_caja", columnNames = {"id", "id_caja"})}, indexes = {
    @Index(name = "ix_corte_caja_caja_fecha", columnList = "id_caja, fecha_hora_apertura"),
    @Index(name = "ix_corte_caja_usuario", columnList = "id_usuario")
})
@Getter
@Setter
public class CorteCaja extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;

    @CurrentTimestamp(event = org.hibernate.generator.EventType.INSERT)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_hora_apertura", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaHoraApertura;

    @Column(name = "fecha_hora_cierre", columnDefinition = "timestamptz")
    private Instant fechaHoraCierre;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "monto_inicial", precision = 18, scale = 2, nullable = false)
    private BigDecimal montoInicial = new BigDecimal("0");

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "total_efectivo", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalEfectivo = new BigDecimal("0");

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "total_tarjeta", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalTarjeta = new BigDecimal("0");

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "total_estimado", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalEstimado = new BigDecimal("0");

    @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2)
    @Column(name = "total_real", precision = 18, scale = 2)
    private BigDecimal totalReal;

    @Setter(AccessLevel.NONE)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "diferencia", precision = 18, scale = 2, insertable = false, updatable = false,
        columnDefinition = "numeric(18,2) generated always as (total_real - total_estimado) stored")
    private BigDecimal diferencia;
}

