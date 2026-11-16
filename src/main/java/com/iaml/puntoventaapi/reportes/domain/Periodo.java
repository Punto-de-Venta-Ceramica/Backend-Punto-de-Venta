package com.iaml.puntoventaapi.reportes.domain;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import java.time.LocalDate;

public enum Periodo {
    SEMANAL,
    MENSUAL;

    public Rango rango(LocalDate fecha) {
        if (this == SEMANAL) {
            LocalDate inicio = fecha.with(previousOrSame(MONDAY));
            return new Rango(inicio, inicio.plusWeeks(1));
        }
        LocalDate inicio = fecha.withDayOfMonth(1);
        return new Rango(inicio, inicio.plusMonths(1));
    }

    public record Rango(LocalDate inicio, LocalDate finExclusivo) {
        public LocalDate ultimoDia() {
            return finExclusivo.minusDays(1);
        }
    }
}
