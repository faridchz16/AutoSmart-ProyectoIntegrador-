package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.ConsumoRequest;
import com.tecsup.autosmart.dto.ConsumosOrdenResponse;
import com.tecsup.autosmart.model.OrdenTrabajo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumoService {

    private final OrdenTrabajoService ordenService;
    private final KardexService kardexService;

    public ConsumoService(OrdenTrabajoService ordenService, KardexService kardexService) {
        this.ordenService = ordenService;
        this.kardexService = kardexService;
    }

    @Transactional
    public ConsumosOrdenResponse registrar(String correoMecanico, Long idOrden, ConsumoRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        kardexService.registrarSalida(orden, req.getIdMaterial(), req.getCantidad(), correoMecanico, req.getReferencia());

        return new ConsumosOrdenResponse(idOrden, orden.getCostoRepuestos(), kardexService.consumosDeOrden(idOrden));
    }

    @Transactional(readOnly = true)
    public ConsumosOrdenResponse listar(String correoMecanico, Long idOrden) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        return new ConsumosOrdenResponse(idOrden, orden.getCostoRepuestos(), kardexService.consumosDeOrden(idOrden));
    }
}