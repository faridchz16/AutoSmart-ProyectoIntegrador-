package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.MetodoPagoResponse;
import com.tecsup.autosmart.mapper.MetodoPagoMapper;
import com.tecsup.autosmart.repository.MetodoPagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MetodoPagoService {

    private final MetodoPagoRepository metodoPagoRepository;
    private final MetodoPagoMapper metodoPagoMapper;

    public MetodoPagoService(MetodoPagoRepository metodoPagoRepository, MetodoPagoMapper metodoPagoMapper) {
        this.metodoPagoRepository = metodoPagoRepository;
        this.metodoPagoMapper = metodoPagoMapper;
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoResponse> listarHabilitados() {
        return metodoPagoRepository.findByHabilitadoTrueOrderByOrdenAsc().stream()
                .map(metodoPagoMapper::toResponse)
                .toList();
    }
}