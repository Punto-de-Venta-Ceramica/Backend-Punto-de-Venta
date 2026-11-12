package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.autenticacion.AutenticacionContrato;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.*;
import com.iaml.puntoventaapi.usuarios.domain.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
            command.sucursalId() != null ? List.of(command.sucursalId()) : List.of(),
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
        return listarPorNegocio(negocioId, incluirAsignacion, null);
    }

    /** Con {@code sucursalId} solo devuelve a los empleados asignados a esa sucursal. */
    @Transactional(readOnly = true)
    public List<EmpleadoDetalle> listarPorNegocio(Long negocioId, boolean incluirAsignacion, Long sucursalId) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(negocioId, propietarioId);
        if (sucursalId != null) {
            verificarSucursalPerteneceANegocio(sucursalId, negocioId);
        }

        return empleados.listarPorNegocio(negocioId).stream()
            .filter(emp -> sucursalId == null || emp.getSucursalIds().contains(sucursalId))
            .map(emp -> incluirAsignacion
                ? detalle(emp)
                : new EmpleadoDetalle(emp, List.of(), null))
            .toList();
    }

    /** Compatibilidad: con sucursal la agrega a las del empleado; con {@code null} quita todas. */
    @Transactional
    public EmpleadoDetalle asignarSucursal(AsignarSucursalCommand command) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), propietarioId);

        Usuario empleado = empleados.buscarPorIdYNegocio(command.empleadoId(), command.negocioId())
            .orElseThrow(() -> new EmpleadoNoEncontradoException(command.empleadoId()));

        List<Long> nuevas = new ArrayList<>();
        if (command.sucursalId() != null) {
            verificarSucursalPerteneceANegocio(command.sucursalId(), command.negocioId());
            nuevas.addAll(empleado.getSucursalIds());
            if (!nuevas.contains(command.sucursalId())) {
                nuevas.add(command.sucursalId());
            }
        }
        return guardarSucursales(empleado, nuevas);
    }

    /** Reemplaza todas las sucursales del empleado por las indicadas (lista vacía: ninguna). */
    @Transactional
    public EmpleadoDetalle actualizarSucursales(ActualizarSucursalesEmpleadoCommand command) {
        Long propietarioId = usuarioAutenticado.obtenerId();
        verificarAccesoNegocio(command.negocioId(), propietarioId);

        Usuario empleado = empleados.buscarPorIdYNegocio(command.empleadoId(), command.negocioId())
            .orElseThrow(() -> new EmpleadoNoEncontradoException(command.empleadoId()));

        List<Long> nuevas = command.sucursalIds().stream().distinct().toList();
        nuevas.forEach(id -> verificarSucursalPerteneceANegocio(id, command.negocioId()));
        return guardarSucursales(empleado, nuevas);
    }

    private EmpleadoDetalle guardarSucursales(Usuario empleado, List<Long> sucursalIds) {
        empleados.actualizarSucursales(empleado.getId(), sucursalIds);
        return detalle(conSucursales(empleado, sucursalIds));
    }

    private Usuario conSucursales(Usuario e, List<Long> sucursalIds) {
        return new Usuario(e.getId(), e.getRolId(), e.getNegocioId(), sucursalIds, e.getNombre(),
            e.getApellidoPaterno(), e.getApellidoMaterno(), e.getCorreo(), e.getTelefono(), e.getPasswordHash());
    }

    private EmpleadoDetalle detalle(Usuario emp) {
        List<SucursalDetalle> sucursalesEmp = emp.getSucursalIds().stream()
            .map(id -> new SucursalDetalle(id, sucursales.obtenerNombrePorId(id).orElse(null)))
            .toList();
        return new EmpleadoDetalle(emp, sucursalesEmp, !sucursalesEmp.isEmpty());
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
            empleado.getSucursalIds(),
            nombreActualizado,
            paternoActualizado,
            maternoActualizado,
            empleado.getCorreo(),
            empleado.getTelefono(),
            empleado.getPasswordHash()
        );

        return detalle(actualizado);
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
