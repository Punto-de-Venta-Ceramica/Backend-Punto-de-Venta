package com.iaml.puntoventaapi.inventario.application;

import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import com.iaml.puntoventaapi.inventario.domain.port.NegocioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepositorioPort inventarios;
    private final NegocioPort negocios;

    @Transactional(readOnly = true)
    public List<ExistenciaProducto> consultarPanel(Long negocioId, Long sucursalId, Long usuarioId) {
        // Un negocio ajeno y uno inexistente producen la misma respuesta pública.
        if (!negocios.perteneceAUsuario(negocioId, usuarioId)) {
            throw new NegocioNoEncontradoException();
        }
        return inventarios.listarPanel(negocioId, sucursalId, usuarioId);
    }

    /** El bloqueo de la fila dura hasta que termina esta transacción: lectura, regla y guardado son atómicos. */
    @Transactional
    public Inventario ajustar(Long inventarioId, int cantidad, Long usuarioId) {
        Inventario inventario = inventarios.buscarParaAjuste(inventarioId, usuarioId)
            .orElseThrow(() -> new InventarioNoEncontradoException());
        inventario.ajustar(cantidad);
        return inventarios.guardar(inventario);
    }
}
