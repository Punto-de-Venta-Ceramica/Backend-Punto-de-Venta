package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DescuentoConAsignaciones;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoGestionPort;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * CRUD de descuentos de un negocio y asignación a productos, categorías y áreas. Todas las
 * operaciones validan que el negocio pertenezca al usuario y que el descuento (y su destino) sean de ese negocio.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DescuentoService {

    private final DescuentoGestionPort descuentos;
    private final NegocioPort negocios;

    @Transactional
    public DescuentoConAsignaciones crear(Long negocioId, GuardarDescuentoCommand command, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        Descuento nuevo = validando(() -> new Descuento(null, negocioId, command.nombre(),
            command.cantidadMinima(), command.cantidadMaxima(), command.porcentaje(), true));
        return new DescuentoConAsignaciones(descuentos.guardar(nuevo), AsignacionesDescuento.vacia());
    }

    public List<DescuentoConAsignaciones> listar(Long negocioId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        List<Descuento> lista = descuentos.listarPorNegocio(negocioId);
        Map<Long, AsignacionesDescuento> asignaciones =
            descuentos.asignacionesDe(lista.stream().map(Descuento::getId).toList());
        return lista.stream().map(d -> new DescuentoConAsignaciones(d, asignaciones.get(d.getId()))).toList();
    }

    public DescuentoConAsignaciones consultar(Long negocioId, Long descuentoId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return detalle(buscar(negocioId, descuentoId));
    }

    @Transactional
    public DescuentoConAsignaciones editar(Long negocioId, Long descuentoId, GuardarDescuentoCommand command,
                                           Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        Descuento descuento = buscar(negocioId, descuentoId);
        validando(() -> {
            descuento.setNombre(command.nombre());
            descuento.modificar(command.cantidadMinima(), command.cantidadMaxima(), command.porcentaje());
            return descuento;
        });
        return new DescuentoConAsignaciones(descuentos.guardar(descuento), descuentos.asignaciones(descuentoId));
    }

    /** Elimina el descuento y sus asignaciones; las ventas y pedidos ya registrados conservan el porcentaje aplicado. */
    @Transactional
    public void eliminar(Long negocioId, Long descuentoId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        buscar(negocioId, descuentoId);
        descuentos.eliminar(descuentoId);
    }

    @Transactional
    public DescuentoConAsignaciones cambiarEstado(Long negocioId, Long descuentoId, boolean activo, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        Descuento descuento = buscar(negocioId, descuentoId);
        if (activo) {
            descuento.activar();
        } else {
            descuento.desactivar();
        }
        return new DescuentoConAsignaciones(descuentos.guardar(descuento), descuentos.asignaciones(descuentoId));
    }

    @Transactional
    public DescuentoConAsignaciones asignar(Long negocioId, Long descuentoId, DestinoDescuento destino,
                                            Long destinoId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        Descuento descuento = buscar(negocioId, descuentoId);
        if (!descuentos.destinoPerteneceAlNegocio(destino, destinoId, negocioId)) {
            throw new CatalogoNoEncontradoException(mensajeDestinoInexistente(destino));
        }
        descuentos.asignar(descuentoId, destino, destinoId);
        return new DescuentoConAsignaciones(descuento, descuentos.asignaciones(descuentoId));
    }

    /** No exige que el destino siga activo: debe poder desasignarse aunque el producto o la categoría se hayan dado de baja. */
    @Transactional
    public DescuentoConAsignaciones quitar(Long negocioId, Long descuentoId, DestinoDescuento destino,
                                           Long destinoId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        Descuento descuento = buscar(negocioId, descuentoId);
        descuentos.quitar(descuentoId, destino, destinoId);
        return new DescuentoConAsignaciones(descuento, descuentos.asignaciones(descuentoId));
    }

    private DescuentoConAsignaciones detalle(Descuento descuento) {
        return new DescuentoConAsignaciones(descuento, descuentos.asignaciones(descuento.getId()));
    }

    private Descuento buscar(Long negocioId, Long descuentoId) {
        return descuentos.buscarPorId(descuentoId, negocioId)
            .orElseThrow(() -> new CatalogoNoEncontradoException("El descuento no existe o no tienes acceso."));
    }

    /** Un negocio ajeno y uno inexistente producen la misma respuesta pública. */
    private void verificarNegocio(Long negocioId, Long usuarioId) {
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new CatalogoNoEncontradoException("El negocio no existe o no tienes acceso.");
        }
    }

    /** El dominio valida con IllegalArgumentException; aquí se traduce a un error de petición inválida. */
    private static <T> T validando(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (IllegalArgumentException ex) {
            throw new DescuentoInvalidoException(ex.getMessage());
        }
    }

    private static String mensajeDestinoInexistente(DestinoDescuento destino) {
        return switch (destino) {
            case PRODUCTO -> "El producto no existe, está dado de baja o no pertenece al negocio.";
            case CATEGORIA -> "La categoría no existe, está dada de baja o no pertenece al negocio.";
            case AREA -> "El área no existe, está dada de baja o no pertenece al negocio.";
        };
    }
}
