package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.MaterialRequest;
import com.tecsup.autosmart.dto.MaterialResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Material;
import com.tecsup.autosmart.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @Transactional
    public MaterialResponse crear(MaterialRequest req) {
        String codigo = normalizarCodigo(req.getCodigo());
        if (materialRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un material con el codigo " + codigo);
        }
        Material material = new Material();
        material.setCodigo(codigo);
        copiarDatos(req, material);
        return aRespuesta(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse actualizar(Long idMaterial, MaterialRequest req) {
        Material material = buscarPorId(idMaterial);
        String codigo = normalizarCodigo(req.getCodigo());
        if (materialRepository.existsByCodigoAndIdMaterialNot(codigo, idMaterial)) {
            throw new ConflictException("Ya existe un material con el codigo " + codigo);
        }
        material.setCodigo(codigo);
        copiarDatos(req, material);
        return aRespuesta(materialRepository.save(material));
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listar(String buscar, boolean soloBajoStock) {
        List<Material> materiales = (buscar == null || buscar.isBlank())
                ? materialRepository.findAllByOrderByNombreAsc()
                : materialRepository.buscar(buscar.trim());
        return materiales.stream()
                .filter(m -> !soloBajoStock || m.isBajoStock())
                .map(this::aRespuesta)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialResponse obtener(Long idMaterial) {
        return aRespuesta(buscarPorId(idMaterial));
    }

    public Material buscarPorId(Long idMaterial) {
        return materialRepository.findById(idMaterial)
                .orElseThrow(() -> new NotFoundException("Material no encontrado"));
    }

    private void copiarDatos(MaterialRequest req, Material material) {
        material.setNombre(req.getNombre().trim());
        material.setUnidad(req.getUnidad().trim().toLowerCase());
        material.setCostoUnitario(req.getCostoUnitario().setScale(2, RoundingMode.HALF_UP));
        material.setStockMinimo(req.getStockMinimo());
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private MaterialResponse aRespuesta(Material m) {
        MaterialResponse res = new MaterialResponse();
        res.setIdMaterial(m.getIdMaterial());
        res.setCodigo(m.getCodigo());
        res.setNombre(m.getNombre());
        res.setUnidad(m.getUnidad());
        res.setCostoUnitario(m.getCostoUnitario());
        res.setStockActual(m.getStockActual());
        res.setStockMinimo(m.getStockMinimo());
        res.setBajoStock(m.isBajoStock());
        return res;
    }
}