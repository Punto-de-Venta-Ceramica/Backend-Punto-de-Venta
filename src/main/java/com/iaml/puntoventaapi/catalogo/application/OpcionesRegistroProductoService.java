package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.port.CategoriasParaRegistroPort;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Categorías necesarias para registrar productos; no permite administrar el catálogo. */
@Service
public class OpcionesRegistroProductoService {
    private final NegocioPort negocios;
    private final CategoriasParaRegistroPort categorias;
    public OpcionesRegistroProductoService(NegocioPort negocios, CategoriasParaRegistroPort categorias) {
        this.negocios = negocios;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<CategoriasParaRegistroPort.Categoria> categorias(long sucursalId, long usuarioId) {
        long negocioId = negocios.negocioDeSucursal(sucursalId, usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Sucursal no disponible."));
        return categorias.listarPorNegocio(negocioId);
    }
}
