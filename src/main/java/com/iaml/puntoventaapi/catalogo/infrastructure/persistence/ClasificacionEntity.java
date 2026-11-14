package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;

/** Datos comunes de las entidades de clasificación del catálogo (área y categoría). */
@MappedSuperclass
@Getter
@Setter
public abstract class ClasificacionEntity extends EntidadConId {

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
