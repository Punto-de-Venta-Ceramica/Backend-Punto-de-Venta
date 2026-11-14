package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.exception.DemasiadosIntentosException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Frena la prueba de contraseñas por fuerza bruta: tras {@link #MAXIMO} fallos seguidos con el mismo correo,
 * ese correo queda bloqueado hasta que pase la {@link #VENTANA}. Un inicio de sesión correcto borra la cuenta.
 * Se lleva por correo y no por IP porque detrás de un proxy todas las peticiones comparten la misma IP.
 * Vive en memoria: basta para una sola instancia y se reinicia con el servidor.
 */
final class ControlIntentos {

    static final int MAXIMO = 8;
    static final Duration VENTANA = Duration.ofMinutes(10);
    private static final int LIMITE_ENTRADAS = 10_000;

    private record Fallos(int cantidad, Instant desde) {
    }

    private final Map<String, Fallos> fallos = new ConcurrentHashMap<>();
    private final Supplier<Instant> ahora;

    ControlIntentos() {
        this(Instant::now);
    }

    ControlIntentos(Supplier<Instant> ahora) {
        this.ahora = ahora;
    }

    /** @throws DemasiadosIntentosException si el correo está bloqueado. */
    void verificar(String correo) {
        Fallos actuales = fallos.get(clave(correo));
        if (actuales != null && vigente(actuales) && actuales.cantidad() >= MAXIMO) {
            throw new DemasiadosIntentosException();
        }
    }

    void registrarFallo(String correo) {
        if (fallos.size() >= LIMITE_ENTRADAS) {
            fallos.values().removeIf(f -> !vigente(f));
        }
        fallos.merge(clave(correo), new Fallos(1, ahora.get()),
            (previos, nuevo) -> vigente(previos) ? new Fallos(previos.cantidad() + 1, previos.desde()) : nuevo);
    }

    void limpiar(String correo) {
        fallos.remove(clave(correo));
    }

    private boolean vigente(Fallos f) {
        return f.desde().plus(VENTANA).isAfter(ahora.get());
    }

    private static String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
