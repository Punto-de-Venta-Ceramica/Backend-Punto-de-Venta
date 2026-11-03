package com.iaml.puntoventaapi.usuarios.infrastructure.security;

import com.iaml.puntoventaapi.usuarios.domain.port.GeneradorPasswordPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GeneradorPasswordAdapter implements GeneradorPasswordPort {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIALES = "!@#$%&*";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;
    private static final int LONGITUD = 10;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generar() {
        char[] password = new char[LONGITUD];
        password[0] = MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length()));
        password[1] = MINUSCULAS.charAt(random.nextInt(MINUSCULAS.length()));
        password[2] = NUMEROS.charAt(random.nextInt(NUMEROS.length()));
        password[3] = ESPECIALES.charAt(random.nextInt(ESPECIALES.length()));

        for (int i = 4; i < LONGITUD; i++) {
            password[i] = TODOS.charAt(random.nextInt(TODOS.length()));
        }

        for (int i = LONGITUD - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }

        return new String(password);
    }
}

