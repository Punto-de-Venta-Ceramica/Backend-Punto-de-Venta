package com.iaml.puntoventaapi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de los tests que prueban SQL real: un PostgreSQL temporal en Docker, misma versión mayor que
 * producción, con el esquema de Hibernate y el script de vistas. Nunca toca la base del negocio.
 * Cada test corre en una transacción que se revierte. Sin Docker, estos tests se omiten.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "spring.config.import=",
    "jwt.secreto=clave-de-pruebas-locales-de-al-menos-32-bytes",
    // update y no create: create borra primero el esquema y, vacío, se detiene por halt_on_error.
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.sql.init.mode=always",
    "spring.devtools.restart.enabled=false"
})
@Transactional
public abstract class PostgresDePrueba {

    /**
     * Un solo contenedor para toda la corrida (arrancarlo es lo que más tarda); Testcontainers lo
     * elimina al terminar la JVM. Se crea perezosamente para que, sin Docker, no falle la carga.
     */
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = iniciar();

    private static PostgreSQLContainer iniciar() {
        var contenedor = new PostgreSQLContainer("postgres:18-alpine");
        if (org.testcontainers.DockerClientFactory.instance().isDockerAvailable()) {
            contenedor.start();
        }
        return contenedor;
    }
}
