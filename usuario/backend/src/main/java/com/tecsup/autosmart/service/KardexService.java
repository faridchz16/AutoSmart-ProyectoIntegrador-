package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.IngresoStockRequest;
import com.tecsup.autosmart.dto.MovimientoResponse;
import com.tecsup.autosmart.mapper.MovimientoMapper;
import com.tecsup.autosmart.model.Material;
import com.tecsup.autosmart.model.MovimientoInventario;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.TipoMovimiento;
import com.tecsup.autosmart.repository.MaterialRepository;
import com.tecsup.autosmart.repository.MovimientoInventarioRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KardexService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final MovimientoInventarioRepository movimientoRepository;
    private final MaterialRepository materialRepository;
    private final MaterialService materialService;
    private final OrdenTrabajoRepository ordenRepository;
    private final MovimientoMapper mapper;

    public KardexService(MovimientoInventarioRepository movimientoRepository,
                         MaterialRepository materialRepository,
                         MaterialService materialService,
                         OrdenTrabajoRepository ordenRepository,
                         MovimientoMapper mapper) {
        this.movimientoRepository = movimientoRepository;
        this.materialRepository = materialRepository;
        this.materialService = materialService;
        this.ordenRepository = ordenRepository;
        this.mapper = mapper;
    }

    @Transactional
    public MovimientoResponse registrarEntrada(String correoAdmin, Long idMaterial, IngresoStockRequest req) {
        Material material = materialService.buscarPorId(idMaterial);

        if (req.getCostoUnitario() != null) {
            material.setCostoUnitario(req.getCostoUnitario().setScale(2, RoundingMode.HALF_UP));
        }
        material.ingresar(req.getCantidad());
        materialRepository.save(material);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMaterial(material);
        movimiento.setTipo(TipoMovimiento.ENTRADA);
        movimiento.setCantidad(req.getCantidad());
        movimiento.setCostoUnitario(material.getCostoUnitario());
        movimiento.setStockResultante(material.getStockActual());
        movimiento.setReferencia(req.getReferencia() == null || req.getReferencia().isBlank()
                ? null : req.getReferencia().trim());
        movimiento.setRegistradoPor(correoAdmin);
        movimiento.setFecha(LocalDateTime.now(ZONA_LIMA).withNano(0));

        return mapper.toResponse(movimientoRepository.save(movimiento));
    }

    @Transactional
    public MovimientoResponse registrarSalida(OrdenTrabajo orden, Long idMaterial, int cantidad,
                                              String correoUsuario, String referencia) {
        Material material = materialService.buscarPorId(idMaterial);
        BigDecimal costo = material.descontar(cantidad);
        materialRepository.saveAndFlush(material);

        orden.sumarCostoRepuestos(costo);
        ordenRepository.save(orden);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMaterial(material);
        movimiento.setTipo(TipoMovimiento.SALIDA);
        movimiento.setCantidad(cantidad);
        movimiento.setCostoUnitario(material.getCostoUnitario());
        movimiento.setStockResultante(material.getStockActual());
        movimiento.setReferencia(referencia == null || referencia.isBlank() ? null : referencia.trim());
        movimiento.setOrden(orden);
        movimiento.setRegistradoPor(correoUsuario);
        movimiento.setFecha(LocalDateTime.now(ZONA_LIMA).withNano(0));

        return mapper.toResponse(movimientoRepository.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> consumosDeOrden(Long idOrden) {
        return movimientoRepository.findByOrden_IdOrdenAndTipoOrderByIdMovimientoAsc(idOrden, TipoMovimiento.SALIDA)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> kardex(Long idMaterial) {
        materialService.buscarPorId(idMaterial);
        return movimientoRepository.findByMaterial_IdMaterialOrderByIdMovimientoAsc(idMaterial).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}