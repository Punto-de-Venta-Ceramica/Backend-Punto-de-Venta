package com.iaml.puntoventaapi.pedidos.domain.port;

import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;

public interface ClientePort {
    long guardar(ClientePedido cliente);
}
