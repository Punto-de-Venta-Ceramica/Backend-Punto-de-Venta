package com.iaml.puntoventaapi.inventario.application;

import com.iaml.puntoventaapi.inventario.InventarioContrato;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        inventarios.listarPanel(negocioId, sucursalId, null, usuarioId)
            .forEach(fila -> existencias.put(fila.productoId(), fila.existencia()));
        return Map.copyOf(existencias);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<FaltanteVenta> descontarParaVenta(Long sucursalId, Map<Long, Integer> cantidades) {
        if (cantidades.isEmpty() || cantidades.values().stream().anyMatch(cantidad -> cantidad == null || cantidad <= 0)) {
            throw new IllegalArgumentException("La venta requiere cantidades positivas.");
        }

        var bloqueados = inventarios.bloquearActivosParaVenta(sucursalId, cantidades.keySet());
        Map<Long, Inventario> porProducto = bloqueados.stream()
            .collect(Collectors.toMap(Inventario::getProductoId, Function.identity()));
        for (var linea : cantidades.entrySet()) {
            Inventario registro = porProducto.get(linea.getKey());
            int disponible = registro == null ? 0 : registro.getExistencia();
            if (linea.getValue() > disponible) {
                return Optional.of(new FaltanteVenta(linea.getKey(), linea.getValue(), disponible));
            }
        }
        for (Inventario registro : bloqueados) {
            registro.ajustar(-cantidades.get(registro.getProductoId()));
            inventarios.guardar(registro);
        }
        return Optional.empty();
    }
}
