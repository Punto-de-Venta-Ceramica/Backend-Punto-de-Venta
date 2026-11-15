package com.iaml.puntoventaapi.autenticacion.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "codigo_recuperacion")
public class CodigoRecuperacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String correo;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    public CodigoRecuperacionEntity() {}

    public CodigoRecuperacionEntity(String correo, String codigo, Instant expiraEn) {
        this.correo = correo;
        this.codigo = codigo;
        this.expiraEn = expiraEn;
    }

    public Long getId() { return id; }
    public String getCorreo() { return correo; }
    public String getCodigo() { return codigo; }
    public Instant getExpiraEn() { return expiraEn; }
}

