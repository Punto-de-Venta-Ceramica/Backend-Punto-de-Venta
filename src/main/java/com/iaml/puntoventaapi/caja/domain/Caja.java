package com.iaml.puntoventaapi.caja.domain;

import com.iaml.puntoventaapi.negocios.domain.Sucursal;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "caja")
@Getter
@Setter
public class Caja extends EntidadConId {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sucursal", nullable = false, unique = true)
    private Sucursal sucursal;
}

