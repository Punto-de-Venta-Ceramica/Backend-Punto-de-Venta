package com.iaml.puntoventaapi.usuarios.domain;

/**
 * Usuario del sistema (propietario o empleado). El rol se referencia por id
 * porque pertenece al módulo de autenticación.
 */
public final class Usuario {

    private final Long id;
    private final Long rolId;
    private final Long negocioId;
    private final java.util.List<Long> sucursalIds;
    private final String nombre;
    private final String apellidoPaterno;
    private final String apellidoMaterno;
    private final String correo;
    private final String telefono;
    private final String passwordHash;
    private final boolean passwordMustChange;

    public Usuario(Long id, Long rolId, String nombre, String apellidoPaterno, String apellidoMaterno,
                   String correo, String telefono, String passwordHash) {
        this(id, rolId, null, null, nombre, apellidoPaterno, apellidoMaterno, correo, telefono, passwordHash, false);
    }

    public Usuario(Long id, Long rolId, Long negocioId, java.util.List<Long> sucursalIds, String nombre, String apellidoPaterno,
                   String apellidoMaterno, String correo, String telefono, String passwordHash) {
        this(id, rolId, negocioId, sucursalIds, nombre, apellidoPaterno, apellidoMaterno, correo, telefono, passwordHash, false);
    }

    public Usuario(Long id, Long rolId, Long negocioId, java.util.List<Long> sucursalIds, String nombre, String apellidoPaterno,
                   String apellidoMaterno, String correo, String telefono, String passwordHash, boolean passwordMustChange) {
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
        this.sucursalIds = sucursalIds == null ? java.util.List.of() : java.util.List.copyOf(sucursalIds);
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correo = correo;
        this.telefono = telefono;
        this.passwordHash = passwordHash;
        this.passwordMustChange = passwordMustChange;
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

    /** Sucursales en las que trabaja el empleado (vacío si no tiene ninguna). */
    public java.util.List<Long> getSucursalIds() {
        return sucursalIds;
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

    public boolean isPasswordMustChange() {
        return passwordMustChange;
    }

    /** Devuelve una nueva instancia del usuario con el hash de contraseña actualizado y sin obligación de cambio. */
    public Usuario conPasswordHash(String nuevoHash) {
        return new Usuario(id, rolId, negocioId, sucursalIds, nombre, apellidoPaterno,
                           apellidoMaterno, correo, telefono, nuevoHash, false);
    }
}
