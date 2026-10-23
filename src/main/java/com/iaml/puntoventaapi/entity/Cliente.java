package com.iaml.puntoventaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@Check(name = "ck_cliente_1", constraints = "btrim(nombre) <> ''")
public class Cliente extends EntidadConId {

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Size(max = 30)
    @Column(name = "telefono", length = 30)
    private String telefono;

    @Size(max = 120)
    @Column(name = "ciudad", length = 120)
    private String ciudad;

    @Size(max = 120)
    @Column(name = "colonia", length = 120)
    private String colonia;

    @Size(max = 200)
    @Column(name = "calle", length = 200)
    private String calle;
}

