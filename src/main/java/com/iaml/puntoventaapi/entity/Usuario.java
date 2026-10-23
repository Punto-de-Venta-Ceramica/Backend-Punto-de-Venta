package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "usuario", indexes = {
    @Index(name = "ix_usuario_rol", columnList = "id_rol")
})
@Getter
@Setter
@Check(name = "ck_usuario_1", constraints = "btrim(nombre) <> ''")
@Check(name = "ck_usuario_2", constraints = "btrim(correo) <> ''")
@Check(name = "ck_usuario_3", constraints = "btrim(password_hash) <> ''")
public class Usuario extends EntidadConId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Size(max = 100)
    @Column(name = "apellido_paterno", length = 100)
    private String apellidoPaterno;

    @Size(max = 100)
    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;

    @NotBlank
    @Size(max = 254)
    @Column(name = "correo", nullable = false, length = 254)
    private String correo;

    @Size(max = 30)
    @Column(name = "telefono", length = 30)
    private String telefono;

    @NotBlank
    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;
}

