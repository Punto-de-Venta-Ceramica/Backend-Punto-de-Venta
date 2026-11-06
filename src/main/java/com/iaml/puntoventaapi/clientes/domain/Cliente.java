package com.iaml.puntoventaapi.clientes.domain;

import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "cliente", check = {
    @CheckConstraint(name = "ck_cliente_1", constraint = "btrim(nombre) <> ''")
})
@Getter
@Setter
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

