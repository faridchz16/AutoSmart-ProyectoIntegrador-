package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.IngresoStockRequest;
import com.tecsup.autosmart.dto.MovimientoResponse;
import com.tecsup.autosmart.model.Material;
import com.tecsup.autosmart.model.MovimientoInventario;
import com.tecsup.autosmart.model.TipoMovimiento;
import com.tecsup.autosmart.repository.MaterialRepository;
import com.tecsup.autosmart.repository.MovimientoInventarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public KardexService(MovimientoInventarioRepository movimientoRepository,
                         MaterialRepository materialRepository,
                         MaterialService materialService) {
        this.movimientoRepository = movimientoRepository;
        this.materialRepository = materialRepository;
        this.materialService = materialService;
    }

    @Transactional
    public MovimientoResponse registrarEntrada(String correoAdmin, Long idMaterial, IngresoStockRequest req) {
        Material material = materialService.buscarPorId(idMaterial);

        if (req.getCostoUnitario() != null) {
            material.setCostoUnitario(req.getCostoUnitario().setScale(2, RoundingMode.HALF_UP));
        }
        material.setStockActual(material.getStockActual() + req.getCantidad());
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

        return aRespuesta(movimientoRepository.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> kardex(Long idMaterial) {
        materialService.buscarPorId(idMaterial);
        return movimientoRepository.findByMaterial_IdMaterialOrderByIdMovimientoAsc(idMaterial).stream()
                .map(this::aRespuesta)
                .collect(Collectors.toList());
    }

    private MovimientoResponse aRespuesta(MovimientoInventario m) {
        MovimientoResponse res = new MovimientoResponse();
        res.setIdMovimiento(m.getIdMovimiento());
        res.setIdMaterial(m.getMaterial().getIdMaterial());
        res.setCodigoMaterial(m.getMaterial().getCodigo());
        res.setTipo(m.getTipo().name());
        res.setCantidad(m.getCantidad());
        res.setCostoUnitario(m.getCostoUnitario());
        res.setStockResultante(m.getStockResultante());
        res.setReferencia(m.getReferencia());
        res.setIdOrden(m.getOrden() == null ? null : m.getOrden().getIdOrden());
        res.setRegistradoPor(m.getRegistradoPor());
        res.setFecha(m.getFecha().toString());
        return res;
    }
}