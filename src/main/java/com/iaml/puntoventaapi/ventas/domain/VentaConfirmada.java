package com.iaml.puntoventaapi.ventas.domain;

import java.time.Instant;
import java.util.Objects;

public record VentaConfirmada(Long ventaId, Long cajaId, Long corteId, Instant fechaHora, Venta venta) {
    public VentaConfirmada {
        Objects.requireNonNull(venta.metodoPago(), "Una venta confirmada requiere un método de pago.");
    }
}
