package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DescuentoConAsignaciones;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoTraslapadoException;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoGestionPort;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
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
        return listar(negocioId, null, usuarioId);
    }

    /** Con productoId, solo los descuentos asignados directamente a ese producto. */
    public List<DescuentoConAsignaciones> listar(Long negocioId, Long productoId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        return conAsignaciones(productoId == null
            ? descuentos.listarPorNegocio(negocioId)
            : descuentos.listarPorProducto(negocioId, productoId));
    }

    /** Una consulta por tipo de destino para toda la lista, no una por descuento. */
    private List<DescuentoConAsignaciones> conAsignaciones(List<Descuento> lista) {
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
        AsignacionesDescuento asignaciones = descuentos.asignaciones(descuentoId);
        validarSinTraslape(descuento, asignaciones);
        return new DescuentoConAsignaciones(descuentos.guardar(descuento), asignaciones);
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
        AsignacionesDescuento asignaciones = descuentos.asignaciones(descuentoId);
        if (activo) {
            descuento.activar();
            validarSinTraslape(descuento, asignaciones);
        } else {
            descuento.desactivar();
        }
        return new DescuentoConAsignaciones(descuentos.guardar(descuento), asignaciones);
    }

    @Transactional
    public DescuentoConAsignaciones asignar(Long negocioId, Long descuentoId, DestinoDescuento destino,
                                            Long destinoId, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        Descuento descuento = buscar(negocioId, descuentoId);
        if (!descuentos.destinoPerteneceAlNegocio(destino, destinoId, negocioId)) {
            throw new CatalogoNoEncontradoException(mensajeDestinoInexistente(destino));
        }
        if (descuento.isActivo()) {
            validarSinTraslape(descuento, destino, List.of(destinoId));
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

    /**
     * Deja los descuentos propios de un producto como indican las líneas, en una sola transacción: elimina
     * los indicados y crea o actualiza cada línea (datos y estado), asignándola al producto. Los traslapes
     * se validan al final, con el estado definitivo, así que el orden de las líneas no importa. Si algo
     * falla no se guarda nada.
     */
    @Transactional
    public List<DescuentoConAsignaciones> guardarDeProducto(Long negocioId, Long productoId,
                                                            Collection<Long> eliminar,
                                                            List<LineaDescuentoCommand> lineas, Long usuarioId) {
        verificarNegocio(negocioId, usuarioId);
        if (!descuentos.destinoPerteneceAlNegocio(DestinoDescuento.PRODUCTO, productoId, negocioId)) {
            throw new CatalogoNoEncontradoException(mensajeDestinoInexistente(DestinoDescuento.PRODUCTO));
        }
        for (Long id : eliminar) {
            buscar(negocioId, id);
            descuentos.eliminar(id);
        }
        List<Descuento> guardados = new ArrayList<>();
        for (LineaDescuentoCommand linea : lineas) {
            Descuento descuento = descuentos.guardar(aplicar(negocioId, linea));
            descuentos.asignar(descuento.getId(), DestinoDescuento.PRODUCTO, productoId);
            guardados.add(descuento);
        }
        List<DescuentoConAsignaciones> resultado = conAsignaciones(guardados);
        resultado.forEach(d -> validarSinTraslape(d.descuento(), d.asignaciones()));
        return resultado;
    }

    /** Descuento nuevo (sin id) o el existente del negocio con los datos y el estado de la línea. */
    private Descuento aplicar(Long negocioId, LineaDescuentoCommand linea) {
        if (linea.id() == null) {
            return validando(() -> new Descuento(null, negocioId, linea.nombre(), linea.cantidadMinima(),
                linea.cantidadMaxima(), linea.porcentaje(), linea.activo()));
        }
        Descuento existente = buscar(negocioId, linea.id());
        return validando(() -> {
            existente.setNombre(linea.nombre());
            existente.modificar(linea.cantidadMinima(), linea.cantidadMaxima(), linea.porcentaje());
            if (linea.activo()) {
                existente.activar();
            } else {
                existente.desactivar();
            }
            return existente;
        });
    }

    /**
     * Los descuentos activos de un mismo producto, categoría o área son escalones por cantidad: sus rangos no
     * pueden traslaparse, para que cada cantidad caiga como mucho en uno (p. ej. 5–10, 11–20, 21 en adelante).
     */
    private void validarSinTraslape(Descuento descuento, AsignacionesDescuento asignaciones) {
        if (!descuento.isActivo()) {
            return;
        }
        // Una consulta por tipo de destino, sin importar cuántos productos, categorías o áreas tenga.
        validarSinTraslape(descuento, DestinoDescuento.PRODUCTO, asignaciones.productoIds());
        validarSinTraslape(descuento, DestinoDescuento.CATEGORIA, asignaciones.categoriaIds());
        validarSinTraslape(descuento, DestinoDescuento.AREA, asignaciones.areaIds());
    }

    private void validarSinTraslape(Descuento descuento, DestinoDescuento destino, Collection<Long> destinoIds) {
        descuentos.activosAsignadosA(destino, destinoIds).values().stream()
            .flatMap(List::stream)
            .filter(otro -> !otro.getId().equals(descuento.getId()))
            .filter(descuento::seTraslapaCon)
            .findFirst()
            .ifPresent(otro -> {
                throw new DescuentoTraslapadoException("Se cruza con «" + otro.getNombre() + "» ("
                    + rango(otro) + ") " + nombreDestino(destino) + ". Ajusta los rangos.");
            });
    }

    private static String rango(Descuento d) {
        return d.getCantidadMaxima() == null
            ? d.getCantidadMinima() + " o más piezas"
            : d.getCantidadMinima() + " a " + d.getCantidadMaxima() + " piezas";
    }

    private static String nombreDestino(DestinoDescuento destino) {
        return switch (destino) {
            case PRODUCTO -> "en el mismo producto";
            case CATEGORIA -> "en la misma categoría";
            case AREA -> "en la misma área";
        };
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
