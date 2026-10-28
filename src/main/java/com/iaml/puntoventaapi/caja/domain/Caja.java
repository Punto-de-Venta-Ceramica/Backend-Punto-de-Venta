package com.iaml.puntoventaapi.caja.domain;

import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.shared.domain.EntidadConId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caja")
@Getter
@Setter
public class Caja extends EntidadConId {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sucursal", nullable = false, unique = true)
    private SucursalEntity sucursal;
}

