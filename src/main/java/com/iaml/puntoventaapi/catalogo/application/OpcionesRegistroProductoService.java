package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.port.CategoriasParaRegistroPort;
import com.iaml.puntoventaapi.catalogo.domain.port.SucursalPort;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Categorías necesarias para registrar productos; no permite administrar el catálogo. */
@Service
public class OpcionesRegistroProductoService {
    private final SucursalPort sucursales;
    private final CategoriasParaRegistroPort categorias;
    public OpcionesRegistroProductoService(SucursalPort sucursales, CategoriasParaRegistroPort categorias) {
        this.sucursales = sucursales;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<CategoriasParaRegistroPort.Categoria> categorias(long sucursalId, long usuarioId) {
        long negocioId = sucursales.negocioAutorizado(sucursalId, usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Sucursal no disponible."));
        return categorias.listarPorNegocio(negocioId);
    }
}
