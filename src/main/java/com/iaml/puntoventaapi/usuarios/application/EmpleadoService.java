package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.autenticacion.AutenticacionContrato;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.*;
import com.iaml.puntoventaapi.usuarios.domain.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpleadoService {

    private static final String ROL_EMPLEADO = "EMPLEADO";

    private final EmpleadoRepositorioPort empleados;
    private final NegocioPort negocios;
    private final SucursalPort sucursales;
    private final UsuarioAutenticadoPort usuarioAutenticado;
    private final AutenticacionContrato autenticacion;
    private final GeneradorPasswordPort generadorPassword;

    public EmpleadoService(EmpleadoRepositorioPort empleados,
                           NegocioPort negocios,
                           SucursalPort sucursales,
                           UsuarioAutenticadoPort usuarioAutenticado,
                           AutenticacionContrato autenticacion,
                           GeneradorPasswordPort generadorPassword) {
        this.empleados = empleados;
        this.negocios = negocios;
        this.sucursales = sucursales;
        this.usuarioAutenticado = usuarioAutenticado;
        this.autenticacion = autenticacion;
        this.generadorPassword = generadorPassword;
    }

    @Transactional
    public EmpleadoCreado registrar(RegistrarEmpleadoCommand command) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), propietarioId);

        String correo = command.correo().trim().toLowerCase();
        if (empleados.existePorCorreo(correo)) {
            throw new CorreoYaRegistradoException();
        }

        if (command.sucursalId() != null) {
            verificarSucursalPerteneceANegocio(command.sucursalId(), command.negocioId());
        }

        Long rolId = autenticacion.buscarRolIdPorNombre(ROL_EMPLEADO)
            .orElseThrow(() -> new IllegalStateException("No existe el rol " + ROL_EMPLEADO));

        String passwordPlana = generadorPassword.generar();
        String passwordHash = autenticacion.hashearPassword(passwordPlana);

        Usuario nuevo = new Usuario(
            null,
            rolId,
            command.negocioId(),
            command.sucursalId(),
            command.nombre().trim(),
            command.apellidoPaterno() != null ? command.apellidoPaterno().trim() : null,
            command.apellidoMaterno() != null ? command.apellidoMaterno().trim() : null,
            correo,
            null,
            passwordHash
        );

        Usuario guardado = empleados.guardar(nuevo);
        String sucursalNombre = command.sucursalId() != null
            ? sucursales.obtenerNombrePorId(command.sucursalId()).orElse(null)
            : null;

        return new EmpleadoCreado(guardado, passwordPlana, sucursalNombre);
    }

    @Transactional(readOnly = true)
    public List<EmpleadoDetalle> listarPorNegocio(Long negocioId, boolean incluirAsignacion) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(negocioId, propietarioId);

        List<Usuario> lista = empleados.listarPorNegocio(negocioId);

        return lista.stream().map(emp -> {
            if (!incluirAsignacion) {
                return new EmpleadoDetalle(emp, null, null);
            }
            Long sucursalId = emp.getSucursalId();
            String sucursalNombre = sucursalId != null
                ? sucursales.obtenerNombrePorId(sucursalId).orElse(null)
                : null;
            return new EmpleadoDetalle(emp, sucursalNombre, sucursalId != null);
        }).toList();
    }

    @Transactional
    public EmpleadoDetalle asignarSucursal(AsignarSucursalCommand command) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), propietarioId);

        Usuario empleado = empleados.buscarPorIdYNegocio(command.empleadoId(), command.negocioId())
            .orElseThrow(() -> new EmpleadoNoEncontradoException(command.empleadoId()));

        if (command.sucursalId() != null) {
            verificarSucursalPerteneceANegocio(command.sucursalId(), command.negocioId());

            // Regla: "siempre y cuando no esté asignado a una sucursal"
            if (empleado.getSucursalId() != null && !empleado.getSucursalId().equals(command.sucursalId())) {
                throw new EmpleadoYaTieneSucursalException("El empleado ya está asignado a otra sucursal. Desasígnalo primero.");
            }
        }

        empleados.actualizarSucursal(command.empleadoId(), command.sucursalId());

        String sucursalNombre = command.sucursalId() != null
            ? sucursales.obtenerNombrePorId(command.sucursalId()).orElse(null)
            : null;

        Usuario actualizado = new Usuario(
            empleado.getId(),
            empleado.getRolId(),
            empleado.getNegocioId(),
            command.sucursalId(),
            empleado.getNombre(),
            empleado.getApellidoPaterno(),
            empleado.getApellidoMaterno(),
            empleado.getCorreo(),
            empleado.getTelefono(),
            empleado.getPasswordHash()
        );

        return new EmpleadoDetalle(actualizado, sucursalNombre, command.sucursalId() != null);
    }

    @Transactional
    public EmpleadoDetalle editar(EditarEmpleadoCommand command) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), propietarioId);

        Usuario empleado = empleados.buscarPorIdYNegocio(command.empleadoId(), command.negocioId())
            .orElseThrow(() -> new EmpleadoNoEncontradoException(command.empleadoId()));

        empleados.actualizarDatos(command.empleadoId(), command.nombre(), command.apellidoPaterno(), command.apellidoMaterno());

        String nombreActualizado = command.nombre() != null && !command.nombre().isBlank() ? command.nombre().trim() : empleado.getNombre();
        String paternoActualizado = command.apellidoPaterno() != null ? command.apellidoPaterno().trim() : empleado.getApellidoPaterno();
        String maternoActualizado = command.apellidoMaterno() != null ? command.apellidoMaterno().trim() : empleado.getApellidoMaterno();

        Usuario actualizado = new Usuario(
            empleado.getId(),
            empleado.getRolId(),
            empleado.getNegocioId(),
            empleado.getSucursalId(),
            nombreActualizado,
            paternoActualizado,
            maternoActualizado,
            empleado.getCorreo(),
            empleado.getTelefono(),
            empleado.getPasswordHash()
        );

        String sucursalNombre = empleado.getSucursalId() != null
            ? sucursales.obtenerNombrePorId(empleado.getSucursalId()).orElse(null)
            : null;

        return new EmpleadoDetalle(actualizado, sucursalNombre, empleado.getSucursalId() != null);
    }

    @Transactional
    public void eliminar(EliminarEmpleadoCommand command) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), propietarioId);

        Usuario empleado = empleados.buscarPorIdYNegocio(command.empleadoId(), command.negocioId())
            .orElseThrow(() -> new EmpleadoNoEncontradoException(command.empleadoId()));

        empleados.eliminar(empleado.getId());
    }

    private void verificarAccesoNegocio(Long negocioId, Long usuarioId) {
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new NegocioNoEncontradoException(negocioId);
        }
    }

    private void verificarSucursalPerteneceANegocio(Long sucursalId, Long negocioId) {
        if (!sucursales.perteneceANegocio(sucursalId, negocioId)) {
            throw new SucursalNoEncontradaException(sucursalId);
        }
    }
}
