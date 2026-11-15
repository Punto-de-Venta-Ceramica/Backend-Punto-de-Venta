package com.iaml.puntoventaapi.ventas.domain;

import java.time.Instant;

public record VentaConfirmada(Long ventaId, Long cajaId, Long corteId, Instant fechaHora, Venta venta) { }
