package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "area", check = {
    @CheckConstraint(name = "ck_area_1", constraint = "btrim(nombre) <> ''")
}, indexes = {
    @Index(name = "ix_area_negocio", columnList = "id_negocio")
})
@Getter
@Setter
public class AreaEntity extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_negocio", nullable = false, foreignKey = @ForeignKey(name = "fk_area_negocio"))
    private NegocioEntity negocio;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private boolean activo = true;

    @Column(name = "fecha_eliminacion", columnDefinition = "timestamp with time zone")
    @Setter(AccessLevel.PACKAGE)
    private Instant fechaEliminacion;
}
