package com.iaml.puntoventaapi.caja.application;

import com.iaml.puntoventaapi.caja.api.dto.*;
import com.iaml.puntoventaapi.caja.domain.Caja;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.caja.domain.exception.*;
import com.iaml.puntoventaapi.caja.infrastructure.persistence.CajaRepository;
import com.iaml.puntoventaapi.caja.infrastructure.persistence.CorteCajaRepository;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalJpaRepository;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CorteCajaService {

    private final CajaRepository cajaRepository;
    private final CorteCajaRepository corteCajaRepository;
    private final SucursalJpaRepository sucursalJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;
    private final SucursalContrato sucursalContrato;
    private final CalculadoraCorte calculadoraCorte;

    public CorteCajaService(
        CajaRepository cajaRepository,
        CorteCajaRepository corteCajaRepository,
        SucursalJpaRepository sucursalJpaRepository,
        UsuarioJpaRepository usuarioJpaRepository,
        SucursalContrato sucursalContrato,
        CalculadoraCorte calculadoraCorte
    ) {
        this.cajaRepository = cajaRepository;
        this.corteCajaRepository = corteCajaRepository;
        this.sucursalJpaRepository = sucursalJpaRepository;
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.sucursalContrato = sucursalContrato;
        this.calculadoraCorte = calculadoraCorte;
    }

    @Transactional
    public CorteCajaResponse abrirCorte(AbrirCorteRequest request, Long usuarioId) {
        Long sucursalId = resolverSucursalId(request.sucursalId(), usuarioId);

        if (corteCajaRepository.existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(sucursalId)) {
            throw new CorteCajaYaAbiertoException("Ya existe un corte de caja abierto para esta sucursal. Debe cerrarlo antes de abrir uno nuevo.");
        }

        Caja caja = obtenerOCrearCaja(sucursalId);
        UsuarioEntity usuario = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado."));

        CorteCaja corte = new CorteCaja();
        corte.setCaja(caja);
        corte.setUsuario(usuario);
        corte.setFechaHoraApertura(Instant.now());
        corte.setMontoInicial(request.montoInicial());
        corte.setTotalEfectivo(BigDecimal.ZERO.setScale(2));
        corte.setTotalTarjeta(BigDecimal.ZERO.setScale(2));
        corte.setTotalEstimado(request.montoInicial());

        corte = corteCajaRepository.save(corte);
        return construirRespuesta(corte);
    }

    @Transactional(readOnly = true)
    public CorteCajaResponse obtenerCorteActivo(Long sucursalIdParam, Long usuarioId) {
        Long sucursalId = resolverSucursalId(sucursalIdParam, usuarioId);

        CorteCaja corte = corteCajaRepository.findByCaja_Sucursal_IdAndFechaHoraCierreIsNull(sucursalId)
            .orElseThrow(() -> new CorteCajaNoAbiertoException("No hay un corte de caja abierto actualmente en esta sucursal."));

        return construirRespuesta(corte);
    }

    @Transactional
    public CorteCajaResponse cerrarCorte(Long corteId, CerrarCorteRequest request, Long usuarioId) {
        CorteCaja corte = corteCajaRepository.findById(corteId)
            .orElseThrow(() -> new CorteCajaNoEncontradoException(corteId));

        resolverSucursalId(corte.getCaja().getSucursal().getId(), usuarioId);

        if (corte.getFechaHoraCierre() != null) {
            throw new CorteCajaYaCerradoException("El corte de caja ya se encuentra cerrado.");
        }

        var calculo = calculadoraCorte.calcular(corte.getId(), corte.getMontoInicial());

        corte.setTotalEfectivo(calculo.totalEfectivo());
        corte.setTotalTarjeta(calculo.totalTarjeta());
        corte.setTotalEstimado(calculo.totalEstimado());
        corte.setTotalReal(request.totalReal());
        corte.setFechaHoraCierre(Instant.now());

        corte = corteCajaRepository.save(corte);
        return construirRespuesta(corte, calculo);
    }

    @Transactional(readOnly = true)
    public CorteCajaResponse consultarCorte(Long corteId, Long usuarioId) {
        CorteCaja corte = corteCajaRepository.findById(corteId)
            .orElseThrow(() -> new CorteCajaNoEncontradoException(corteId));

        resolverSucursalId(corte.getCaja().getSucursal().getId(), usuarioId);
        return construirRespuesta(corte);
    }

    @Transactional(readOnly = true)
    public List<ResumenCorteCajaDto> listarHistorial(Long sucursalIdParam, Long usuarioId) {
        Long sucursalId = resolverSucursalId(sucursalIdParam, usuarioId);

        List<CorteCaja> cortes = corteCajaRepository.findByCaja_Sucursal_IdOrderByFechaHoraAperturaDesc(sucursalId);

        return cortes.stream().map(c -> {
            boolean cerrado = c.getFechaHoraCierre() != null;
            BigDecimal diferencia = c.getTotalReal() != null && c.getTotalEstimado() != null
                ? c.getTotalReal().subtract(c.getTotalEstimado())
                : null;
            String sucursalNombre = c.getCaja() != null && c.getCaja().getSucursal() != null
                ? c.getCaja().getSucursal().getNombre()
                : null;
            String usuarioNombre = c.getUsuario() != null ? c.getUsuario().getNombre() : null;

            return new ResumenCorteCajaDto(
                c.getId(),
                c.getCaja() != null ? c.getCaja().getId() : null,
                c.getCaja() != null && c.getCaja().getSucursal() != null ? c.getCaja().getSucursal().getId() : null,
                sucursalNombre,
                c.getUsuario() != null ? c.getUsuario().getId() : null,
                usuarioNombre,
                c.getFechaHoraApertura(),
                c.getFechaHoraCierre(),
                cerrado,
                c.getMontoInicial(),
                c.getTotalEstimado(),
                c.getTotalEstimado(),
                c.getTotalReal(),
                diferencia
            );
        }).toList();
    }

    public Long resolverSucursalId(Long sucursalId, Long usuarioId) {
        UsuarioEntity usuario = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado o no encontrado."));

        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";

        if ("EMPLEADO".equalsIgnoreCase(rol)) {
            List<Long> asignadas = usuario.getSucursales().stream().map(SucursalEntity::getId).toList();
            if (asignadas.isEmpty()) {
                throw new AccessDeniedException("El empleado no tiene asignada una sucursal.");
            }
            if (sucursalId != null) {
                if (!asignadas.contains(sucursalId)) {
                    throw new AccessDeniedException("No tienes permiso para acceder a una sucursal distinta a la asignada.");
                }
                return sucursalId;
            }
            if (asignadas.size() == 1) {
                return asignadas.get(0);
            }
            throw new IllegalArgumentException("Debes especificar el ID de la sucursal para el corte de caja.");
        }

        if ("PROPIETARIO".equalsIgnoreCase(rol)) {
            if (sucursalId != null) {
                if (sucursalContrato.negocioAutorizado(sucursalId, usuarioId).isEmpty()) {
                    throw new AccessDeniedException("No tienes permiso para acceder a esta sucursal.");
                }
                return sucursalId;
            }

            Long negocioId = usuario.getNegocio() != null ? usuario.getNegocio().getId() : null;
            if (negocioId != null) {
                List<SucursalEntity> sucursales = sucursalJpaRepository
                    .findByNegocio_IdAndNegocio_Usuarios_IdAndNegocio_ActivoTrueOrderByIdAsc(negocioId, usuarioId);
                if (sucursales.size() == 1) {
                    return sucursales.get(0).getId();
                }
            }
            throw new IllegalArgumentException("Debes especificar el ID de la sucursal para el corte de caja.");
        }

        throw new AccessDeniedException("Rol no autorizado para operaciones de corte de caja.");
    }

    private Caja obtenerOCrearCaja(Long sucursalId) {
        return cajaRepository.findBySucursal_Id(sucursalId)
            .orElseGet(() -> {
                SucursalEntity sucursal = sucursalJpaRepository.findById(sucursalId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró la sucursal con ID: " + sucursalId));
                Caja nuevaCaja = new Caja();
                nuevaCaja.setSucursal(sucursal);
                return cajaRepository.save(nuevaCaja);
            });
    }

    private CorteCajaResponse construirRespuesta(CorteCaja corte) {
        var calculo = calculadoraCorte.calcular(corte.getId(), corte.getMontoInicial());
        return construirRespuesta(corte, calculo);
    }

    private CorteCajaResponse construirRespuesta(CorteCaja corte, CalculadoraCorte.ResultadoCalculo calculo) {
        boolean cerrado = corte.getFechaHoraCierre() != null;
        BigDecimal diferencia = corte.getTotalReal() != null && corte.getTotalEstimado() != null
            ? corte.getTotalReal().subtract(corte.getTotalEstimado())
            : null;

        String sucursalNombre = corte.getCaja() != null && corte.getCaja().getSucursal() != null
            ? corte.getCaja().getSucursal().getNombre()
            : null;
        Long sucursalId = corte.getCaja() != null && corte.getCaja().getSucursal() != null
            ? corte.getCaja().getSucursal().getId()
            : null;
        String usuarioNombre = corte.getUsuario() != null
            ? corte.getUsuario().getNombre()
            : null;
        Long usuarioId = corte.getUsuario() != null
            ? corte.getUsuario().getId()
            : null;

        return new CorteCajaResponse(
            corte.getId(),
            corte.getCaja() != null ? corte.getCaja().getId() : null,
            sucursalId,
            sucursalNombre,
            usuarioId,
            usuarioNombre,
            corte.getFechaHoraApertura(),
            corte.getFechaHoraCierre(),
            cerrado,
            corte.getMontoInicial(),
            calculo.totalVentas(),
            calculo.totalAnticipos(),
            calculo.totalLiquidaciones(),
            calculo.totalCancelaciones(),
            calculo.ventasEfectivo(),
            calculo.ventasTarjeta(),
            calculo.cancelacionesPedidos(),
            calculo.totalEfectivo(),
            calculo.totalTarjeta(),
            calculo.totalEstimado(),
            corte.getTotalReal(),
            diferencia,
            calculo.productos()
        );
    }
}
