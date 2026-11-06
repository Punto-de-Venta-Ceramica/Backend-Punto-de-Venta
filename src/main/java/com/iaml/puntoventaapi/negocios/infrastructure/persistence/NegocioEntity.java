package com.iaml.puntoventaapi.negocios.infrastructure.persistence;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "Negocio")
@Table(name = "negocio", check = {
    @CheckConstraint(name = "ck_negocio_1", constraint = "btrim(nombre) <> ''")
})
@Getter
@Setter
public class NegocioEntity extends EntidadConId {

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "logo", columnDefinition = "text")
    private String logo;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_creacion", nullable = false, updatable = false,
        columnDefinition = "timestamp with time zone")
    @Setter(AccessLevel.NONE)
    private Instant fechaCreacion;

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_actualizacion", nullable = false,
        columnDefinition = "timestamp with time zone")
    @Setter(AccessLevel.NONE)
    private Instant fechaActualizacion;

    @Column(name = "fecha_eliminacion", columnDefinition = "timestamp with time zone")
    @Setter(AccessLevel.PACKAGE)
    private Instant fechaEliminacion;

    @ColumnDefault("true")
    @Column(name = "activo", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private boolean activo = true;

    // Impide que una edición concurrente sobrescriba la eliminación lógica.
    @Version
    @ColumnDefault("0")
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "negocio_usuario", joinColumns = @JoinColumn(name = "id_negocio"),
        inverseJoinColumns = @JoinColumn(name = "id_usuario"),
        indexes = @Index(name = "ix_negocio_usuario_usuario", columnList = "id_usuario"))
    private Set<UsuarioEntity> usuarios = new HashSet<>();

}

