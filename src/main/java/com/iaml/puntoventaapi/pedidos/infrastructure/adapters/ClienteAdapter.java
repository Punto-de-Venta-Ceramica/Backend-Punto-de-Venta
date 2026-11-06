package com.iaml.puntoventaapi.pedidos.infrastructure.adapters;

import com.iaml.puntoventaapi.clientes.ClientesContrato;
import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ClientePort;
import org.springframework.stereotype.Component;

@Component
public class ClienteAdapter implements ClientePort {
    private final ClientesContrato clientes;
    public ClienteAdapter(ClientesContrato clientes) { this.clientes = clientes; }
    @Override
    public long guardar(ClientePedido cliente) {
        return clientes.registrarParaPedido(new ClientesContrato.Datos(cliente.nombre(), cliente.telefono(),
            cliente.ciudad(), cliente.colonia(), cliente.calle()));
    }
}
