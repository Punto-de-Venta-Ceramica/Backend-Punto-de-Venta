package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

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
public class DetallePedidoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_pedido")
    private Long pedidoId;

    @Column(name = "id_producto")
    private Long productoId;
}

