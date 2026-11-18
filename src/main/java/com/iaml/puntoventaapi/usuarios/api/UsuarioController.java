package com.iaml.puntoventaapi.usuarios.api;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.usuarios.api.dto.ActualizarPerfilRequest;
import com.iaml.puntoventaapi.usuarios.api.dto.PerfilUsuarioResponse;
import com.iaml.puntoventaapi.usuarios.api.dto.RegistrarPropietarioRequest;
import com.iaml.puntoventaapi.usuarios.api.dto.UsuarioResponse;
import com.iaml.puntoventaapi.usuarios.application.PropietarioService;
import com.iaml.puntoventaapi.usuarios.domain.exception.CorreoYaRegistradoException;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Usuarios", description = "Registro y perfil de usuarios")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PropietarioService propietarioService;
    private final UsuarioActual usuarioActual;
    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioController(PropietarioService propietarioService,
                             UsuarioActual usuarioActual,
                             UsuarioJpaRepository usuarioJpaRepository) {
        this.propietarioService = propietarioService;
        this.usuarioActual = usuarioActual;
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Operation(summary = "Registrar propietario", description = "Crea un nuevo usuario propietario.")
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrarPropietario(@Valid @RequestBody RegistrarPropietarioRequest request) {
        return UsuarioResponse.from(propietarioService.registrar(request.toCommand()));
    }

    @Operation(summary = "Obtener perfil", description = "Consulta los datos del usuario autenticado actual.")
    @GetMapping("/perfil")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PerfilUsuarioResponse obtenerPerfil() {
        Long usuarioId = usuarioActual.id();
        UsuarioEntity entity = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return PerfilUsuarioResponse.from(entity);
    }

    @Operation(summary = "Actualizar perfil", description = "Actualiza los datos personales (nombre, apellidos, teléfono, correo) del usuario autenticado.")
    @PutMapping("/perfil")
    @org.springframework.transaction.annotation.Transactional
    public PerfilUsuarioResponse actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request) {
        Long usuarioId = usuarioActual.id();
        UsuarioEntity entity = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (request.correo() != null && !request.correo().isBlank()) {
            String nuevoCorreo = request.correo().trim();
            if (!nuevoCorreo.equalsIgnoreCase(entity.getCorreo().trim())) {
                usuarioJpaRepository.findByCorreoNormalizado(nuevoCorreo).ifPresent(existente -> {
                    if (!existente.getId().equals(usuarioId)) {
                        throw new CorreoYaRegistradoException("El correo electrónico ya está registrado por otro usuario.");
                    }
                });
                entity.setCorreo(nuevoCorreo);
            }
        }

        entity.setNombre(request.nombre().trim());
        entity.setApellidoPaterno(request.apellidoPaterno() != null && !request.apellidoPaterno().isBlank() ? request.apellidoPaterno().trim() : null);
        entity.setApellidoMaterno(request.apellidoMaterno() != null && !request.apellidoMaterno().isBlank() ? request.apellidoMaterno().trim() : null);
        entity.setTelefono(request.telefono() != null && !request.telefono().isBlank() ? request.telefono().trim() : null);

        UsuarioEntity guardado = usuarioJpaRepository.saveAndFlush(entity);
        return PerfilUsuarioResponse.from(guardado);
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ProblemDetail correoYaRegistrado(CorreoYaRegistradoException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("errores", java.util.Map.of("correo", ex.getMessage()));
        return problem;
    }
}
