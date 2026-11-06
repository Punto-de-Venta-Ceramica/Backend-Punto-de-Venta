package com.iaml.puntoventaapi.usuarios.domain;

/**
 * Usuario del sistema (propietario o empleado). El rol se referencia por id
 * porque pertenece al módulo de autenticación.
 */
public final class Usuario {

    private final Long id;
    private final Long rolId;
    private final Long negocioId;
    private final Long sucursalId;
    private final String nombre;
    private final String apellidoPaterno;
    private final String apellidoMaterno;
    private final String correo;
    private final String telefono;
    private final String passwordHash;

    public Usuario(Long id, Long rolId, String nombre, String apellidoPaterno, String apellidoMaterno,
                   String correo, String telefono, String passwordHash) {
        this(id, rolId, null, null, nombre, apellidoPaterno, apellidoMaterno, correo, telefono, passwordHash);
    }

    public Usuario(Long id, Long rolId, Long negocioId, Long sucursalId, String nombre, String apellidoPaterno,
                   String apellidoMaterno, String correo, String telefono, String passwordHash) {
        if (rolId == null) {
            throw new IllegalArgumentException("El usuario debe tener rol");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El usuario debe tener nombre");
        }
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El usuario debe tener correo");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El usuario debe tener contraseña");
        }
        this.id = id;
        this.rolId = rolId;
        this.negocioId = negocioId;
        this.sucursalId = sucursalId;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correo = correo;
        this.telefono = telefono;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public Long getRolId() {
        return rolId;
    }

    public Long getNegocioId() {
        return negocioId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
