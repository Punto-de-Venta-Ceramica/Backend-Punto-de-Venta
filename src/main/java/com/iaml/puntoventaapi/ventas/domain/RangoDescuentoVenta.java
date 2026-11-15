package com.iaml.puntoventaapi.ventas.domain;

import java.math.BigDecimal;

public record RangoDescuentoVenta(int cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) { }
