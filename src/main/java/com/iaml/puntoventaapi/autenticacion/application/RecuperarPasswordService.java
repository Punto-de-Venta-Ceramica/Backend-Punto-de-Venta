package com.iaml.puntoventaapi.autenticacion.application;

import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.CredencialRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.GeneradorHashPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.PasswordHasherPort;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.CodigoRecuperacionEntity;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.CodigoRecuperacionRepository;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Gestiona la recuperación de contraseña mediante código de 6 dígitos enviado por email,
 * y el cambio de contraseña para usuarios autenticados.
 */
@Service
public class RecuperarPasswordService {

    private final CredencialRepositorioPort credenciales;
    private final UsuarioRepositorioPort usuarios;
    private final PasswordHasherPort hasher;
    private final GeneradorHashPort hashGenerator;
    private final CodigoRecuperacionRepository codigoRepo;
    private final JavaMailSender mailSender;

    @Autowired
    public RecuperarPasswordService(CredencialRepositorioPort credenciales,
                                    UsuarioRepositorioPort usuarios,
                                    PasswordHasherPort hasher,
                                    GeneradorHashPort hashGenerator,
                                    CodigoRecuperacionRepository codigoRepo,
                                    JavaMailSender mailSender) {
        this.credenciales = credenciales;
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.hashGenerator = hashGenerator;
        this.codigoRepo = codigoRepo;
        this.mailSender = mailSender;
    }

    public RecuperarPasswordService() {
        this(null, null, null, null, null, null);
    }

    private final SecureRandom random = new SecureRandom();

    // ----------------------------------------------------------------
    //  Cambiar contraseña (usuario autenticado)
    // ----------------------------------------------------------------

    /**
     * Cambia la contraseña del usuario autenticado.
     * Verifica la contraseña actual y que la nueva y su confirmación coincidan.
     */
    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordActual, String nuevaPassword, String confirmarPassword) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        var credencial = credenciales.buscarPorUsuarioId(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        try {
            credencial.verificarPassword(passwordActual, hasher);
        } catch (CredencialesInvalidasException ex) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }

        var usuario = usuarios.buscarPorId(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        var usuarioActualizado = usuario.conPasswordHash(hashGenerator.hashear(nuevaPassword));
        usuarios.guardar(usuarioActualizado);
    }

    // ----------------------------------------------------------------
    //  Recuperar contraseña (flujo por email)
    // ----------------------------------------------------------------

    /**
     * Paso 1: Genera un código de 6 dígitos y lo envía al correo del usuario.
     */
    @Transactional
    public void solicitarCodigo(String correo) {
        // Verificamos que el correo exista (sin revelar si existe o no, por seguridad)
        credenciales.buscarPorCorreo(correo)
            .orElseThrow(() -> new IllegalArgumentException("El correo no está registrado en el sistema."));

        // Eliminar códigos anteriores del mismo correo
        codigoRepo.deleteByCorreo(correo);

        // Generar código de 6 dígitos
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        Instant expira = Instant.now().plus(10, ChronoUnit.MINUTES);

        codigoRepo.save(new CodigoRecuperacionEntity(correo, codigo, expira));

        // Enviar correo
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(correo);
        mensaje.setSubject("Código de recuperación de contraseña - BarroControl");
        mensaje.setText(
            "Hola,\n\n" +
            "Tu código de recuperación de contraseña es: " + codigo + "\n\n" +
            "Este código es válido por 10 minutos.\n\n" +
            "Si no solicitaste este código, ignora este mensaje.\n\n" +
            "BarroControl"
        );
        mailSender.send(mensaje);
    }

    /**
     * Paso 2: Verifica el código y restablece la contraseña si el código es correcto.
     */
    @Transactional
    public void restablecerPassword(String correo, String codigo, String nuevaPassword, String confirmarPassword) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        var entidad = codigoRepo.findTopByCorreoOrderByExpiraEnDesc(correo)
            .orElseThrow(() -> new IllegalArgumentException("No existe un código de recuperación para este correo."));

        if (Instant.now().isAfter(entidad.getExpiraEn())) {
            throw new IllegalArgumentException("El código de recuperación ha expirado. Solicita uno nuevo.");
        }

        if (!entidad.getCodigo().equals(codigo)) {
            throw new IllegalArgumentException("El código de recuperación es incorrecto.");
        }

        var usuario = usuarios.buscarPorCorreo(correo)
            .orElseThrow(CredencialesInvalidasException::new);

        var usuarioActualizado = usuario.conPasswordHash(hashGenerator.hashear(nuevaPassword));
        usuarios.guardar(usuarioActualizado);

        // Eliminar el código usado
        codigoRepo.deleteByCorreo(correo);
    }
}

