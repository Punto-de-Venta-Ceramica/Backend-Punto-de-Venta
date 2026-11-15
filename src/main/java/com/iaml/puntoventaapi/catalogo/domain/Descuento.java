package com.iaml.puntoventaapi.catalogo.domain;

import java.math.BigDecimal;

/**
 * Descuento por volumen: se aplica cuando la cantidad comprada está entre cantidadMinima y
 * cantidadMaxima (sin tope si es null).
 */
public final class Descuento {

    private static final int NOMBRE_MAX = 120;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final Long id;
    private final Long negocioId;
    private String nombre;
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private BigDecimal porcentaje;
    private boolean activo;

    public Descuento(Long id, Long negocioId, String nombre, Integer cantidadMinima, Integer cantidadMaxima,
                     BigDecimal porcentaje, boolean activo) {
        if (negocioId == null || negocioId <= 0) {
            throw new IllegalArgumentException("El negocio debe tener un identificador positivo.");
        }
        this.id = id;
        this.negocioId = negocioId;
        setNombre(nombre);
        modificar(cantidadMinima, cantidadMaxima, porcentaje);
        this.activo = activo;
    }

    /**
     * Cambia el rango y el porcentaje a la vez: la regla máxima >= mínima compara ambos campos,
     * por eso no hay setters sueltos para ellos.
     */
    public void modificar(Integer cantidadMinima, Integer cantidadMaxima, BigDecimal porcentaje) {
        if (cantidadMinima == null || cantidadMinima < 1) {
            throw new IllegalArgumentException("La cantidad mínima debe ser al menos 1.");
        }
        if (cantidadMaxima != null && cantidadMaxima < cantidadMinima) {
            throw new IllegalArgumentException("La cantidad máxima no puede ser menor que la mínima.");
        }
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(CIEN) > 0
                || porcentaje.scale() > 2) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 100, con máximo 2 decimales.");
        }
        this.cantidadMinima = cantidadMinima;
        this.cantidadMaxima = cantidadMaxima;
        this.porcentaje = porcentaje;
    }

    /**
     * Dos rangos se traslapan si alguna cantidad cae en ambos. Un mismo destino no puede tener dos
     * descuentos activos traslapados: así cada cantidad cae como mucho en un escalón.
     */
    public boolean seTraslapaCon(Descuento otro) {
        boolean otroEmpiezaAntesDeQueTermine = cantidadMaxima == null || otro.cantidadMinima <= cantidadMaxima;
        boolean empiezaAntesDeQueOtroTermine = otro.cantidadMaxima == null || cantidadMinima <= otro.cantidadMaxima;
        return otroEmpiezaAntesDeQueTermine && empiezaAntesDeQueOtroTermine;
    }

    public Long getId() {
        return id;
    }

    public Long getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > NOMBRE_MAX) {
            throw new IllegalArgumentException(
                "El nombre del descuento es obligatorio y no debe superar " + NOMBRE_MAX + " caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public Integer getCantidadMinima() {
        return cantidadMinima;
    }

    public Integer getCantidadMaxima() {
        return cantidadMaxima;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public boolean isActivo() {
        return activo;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}
