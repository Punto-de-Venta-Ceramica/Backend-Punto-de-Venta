package com.iaml.puntoventaapi.negocios.application;

import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.negocios.domain.port.NegocioRepositorioPort;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NegociosContratoService implements NegociosContrato {

    private final NegocioRepositorioPort negocios;

    public NegociosContratoService(NegocioRepositorioPort negocios) {
        this.negocios = negocios;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return negocioId != null && negocioId > 0 && usuarioId != null && usuarioId > 0
            && negocios.perteneceAUsuario(negocioId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> eslogan(Long negocioId) {
        if (negocioId == null || negocioId <= 0) return Optional.empty();
        return negocios.eslogan(negocioId);
    }
}
