package com.iaml.puntoventaapi.clientes;

public interface ClientesContrato {
    record Datos(String nombre, String telefono, String ciudad, String colonia, String calle) { }
    long registrarParaPedido(Datos datos);
}
