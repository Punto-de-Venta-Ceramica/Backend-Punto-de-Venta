package com.iaml.puntoventaapi.autenticacion.infrastructure.security;

import com.iaml.puntoventaapi.autenticacion.domain.port.GeneradorHashPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.PasswordHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasherPort, GeneradorHashPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean coincide(String passwordPlano, String passwordHash) {
        return passwordEncoder.matches(passwordPlano, passwordHash);
    }

    @Override
    public String hashear(String passwordPlano) {
        return passwordEncoder.encode(passwordPlano);
    }
}
