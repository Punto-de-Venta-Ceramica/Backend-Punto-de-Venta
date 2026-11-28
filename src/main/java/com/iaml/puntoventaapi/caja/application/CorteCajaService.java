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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        CorteCaja corte = corteCajaRepository.buscarConBloqueo(corteId)
            .orElseThrow(() -> new CorteCajaNoEncontradoException(corteId));

        resolverSucursalId(corte.getCaja().getSucursal().getId(), usuarioId);

        if (corte.getFechaHoraCierre() != null) {
            throw new CorteCajaYaCerradoException("El corte de caja ya se encuentra cerrado.");
        }

        if (corte.getUsuario() != null && !corte.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("Únicamente el usuario que abrió el corte de caja puede cerrarlo.");
        }

        var calculo = calculadoraCorte.calcular(corte.getId(), corte.getMontoInicial());

        corte.setTotalEfectivo(calculo.totalEfectivo());
        corte.setTotalTarjeta(calculo.totalTarjeta());
        // El arqueo y faltante/sobrante en caja solo contemplan el efectivo (no tarjeta).
        corte.setTotalEstimado(calculo.totalEfectivo());
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

        UsuarioEntity usuario = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado o no encontrado."));

        if ("EMPLEADO".equalsIgnoreCase(usuario.getRol().getNombre())) {
            if (!corte.getUsuario().getId().equals(usuarioId)) {
                throw new AccessDeniedException("No tienes permiso para consultar cortes de caja de otros usuarios.");
            }
        }

        return construirRespuesta(corte);
    }

    @Transactional(readOnly = true)
    public Page<ResumenCorteCajaDto> listarHistorial(Long sucursalIdParam, Long usuarioId, Pageable pageable) {
        Pageable ordenado = pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                             Sort.by(Sort.Direction.DESC, "fechaHoraApertura"));

        UsuarioEntity usuario = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado o no encontrado."));

        String rol = usuario.getRol().getNombre();

        Page<CorteCaja> cortes;

        if ("EMPLEADO".equalsIgnoreCase(rol)) {
            if (sucursalIdParam != null) {
                Long sucursalEmpleadoId = resolverSucursalId(sucursalIdParam, usuarioId);
                cortes = corteCajaRepository.findByCaja_Sucursal_IdAndUsuario_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(
                    sucursalEmpleadoId, usuarioId, ordenado);
            } else {
                List<Long> asignadas = usuario.getSucursales().stream().map(SucursalEntity::getId).toList();
                if (asignadas.isEmpty()) {
                    throw new AccessDeniedException("El empleado no tiene asignada una sucursal.");
                }
                if (asignadas.size() == 1) {
                    cortes = corteCajaRepository.findByCaja_Sucursal_IdAndUsuario_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(
                        asignadas.get(0), usuarioId, ordenado);
                } else {
                    cortes = corteCajaRepository.findByUsuario_IdAndCaja_Sucursal_IdInAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(
                        usuarioId, asignadas, ordenado);
                }
            }
        } else if ("PROPIETARIO".equalsIgnoreCase(rol)) {
            if (sucursalIdParam != null) {
                if (sucursalContrato.negocioAutorizado(sucursalIdParam, usuarioId).isEmpty()) {
                    throw new AccessDeniedException("No tienes permiso para acceder a esta sucursal.");
                }
                cortes = corteCajaRepository.findByCaja_Sucursal_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(sucursalIdParam, ordenado);
            } else {
                Long negocioId = usuario.getNegocio() != null ? usuario.getNegocio().getId() : null;
                if (negocioId != null) {
                    cortes = corteCajaRepository.findByCaja_Sucursal_Negocio_IdAndFechaHoraCierreIsNotNullOrderByFechaHoraAperturaDesc(negocioId, ordenado);
                } else {
                    cortes = Page.empty(ordenado);
                }
            }
        } else {
            throw new AccessDeniedException("Rol no autorizado para operaciones de corte de caja.");
        }

        return cortes.map(c -> {
            boolean cerrado = c.getFechaHoraCierre() != null;
            BigDecimal efectivoEsperado = c.getTotalEfectivo() != null ? c.getTotalEfectivo() : c.getTotalEstimado();
            BigDecimal diferencia = c.getTotalReal() != null
                ? c.getTotalReal().subtract(efectivoEsperado)
                : null;
            return new ResumenCorteCajaDto(
                c.getId(),
                c.getCaja().getId(),
                c.getCaja().getSucursal().getId(),
                c.getCaja().getSucursal().getNombre(),
                c.getUsuario().getId(),
                c.getUsuario().getNombre(),
                c.getFechaHoraApertura(),
                c.getFechaHoraCierre(),
                cerrado,
                c.getMontoInicial(),
                c.getTotalEstimado(),
                efectivoEsperado,
                c.getTotalReal(),
                diferencia
            );
        });
    }

    public Long resolverSucursalId(Long sucursalId, Long usuarioId) {
        UsuarioEntity usuario = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado o no encontrado."));

        String rol = usuario.getRol().getNombre();

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
                // resolverSucursalId ya comprobó que la sucursal existe y es accesible.
                Caja nuevaCaja = new Caja();
                nuevaCaja.setSucursal(sucursalJpaRepository.getReferenceById(sucursalId));
                return cajaRepository.save(nuevaCaja);
            });
    }

    private CorteCajaResponse construirRespuesta(CorteCaja corte) {
        var calculo = calculadoraCorte.calcular(corte.getId(), corte.getMontoInicial());
        return construirRespuesta(corte, calculo);
    }

    private CorteCajaResponse construirRespuesta(CorteCaja corte, CalculadoraCorte.ResultadoCalculo calculo) {
        boolean cerrado = corte.getFechaHoraCierre() != null;
        BigDecimal diferencia = corte.getTotalReal() != null
            ? corte.getTotalReal().subtract(calculo.totalEfectivo())
            : null;

        return new CorteCajaResponse(
            corte.getId(),
            corte.getCaja().getId(),
            corte.getCaja().getSucursal().getId(),
            corte.getCaja().getSucursal().getNombre(),
            corte.getUsuario().getId(),
            corte.getUsuario().getNombre(),
            corte.getFechaHoraApertura(),
            corte.getFechaHoraCierre(),
            cerrado,
            corte.getMontoInicial(),
            calculo.totalVentas(),
            calculo.totalAnticipos(),
            calculo.totalLiquidaciones(),
            calculo.totalCancelaciones(),
            calculo.totalNeto(),
            calculo.ventasEfectivo(),
            calculo.ventasTarjeta(),
            calculo.cancelacionesPedidos(),
            calculo.totalEfectivo(),
            calculo.totalTarjeta(),
            calculo.totalEstimado(),
            corte.getTotalReal(),
            diferencia,
            calculo.productos(),
            calculadoraCorte.pedidos(corte.getId())
        );
    }
}
