package com.iaml.puntoventaapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DetalleVentaId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_venta")
    private Long ventaId;

    @Column(name = "id_producto")
    private Long productoId;
}

