package com.iaml.puntoventaapi.inventario.application;

import com.iaml.puntoventaapi.inventario.InventarioContrato;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class InventarioContratoService implements InventarioContrato {

    private final InventarioRepositorioPort inventarios;

    @Override
    @Transactional
    public void registrarExistenciasIniciales(Long productoId, Map<Long, Integer> existenciaPorSucursal) {
        existenciaPorSucursal.forEach((sucursalId, existencia) ->
            inventarios.guardar(new Inventario(null, sucursalId, productoId, existencia)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> consultarExistencias(Long negocioId, Long sucursalId, Long usuarioId) {
        Map<Long, Integer> existencias = new HashMap<>();
        inventarios.listarPanel(negocioId, sucursalId, usuarioId)
            .forEach(fila -> existencias.put(fila.productoId(), fila.existencia()));
        return Map.copyOf(existencias);
    }
}
