package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.ListaPagosResponse;
import com.tecsup.autosmart.dto.PagoAdminResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.mapper.PagoMapper;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.Pago;
import com.tecsup.autosmart.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminPagoService {

    private final PagoRepository pagoRepository;
    private final PagoMapper pagoMapper;
    private final Clock clock;

    public AdminPagoService(PagoRepository pagoRepository, PagoMapper pagoMapper, Clock clock) {
        this.pagoRepository = pagoRepository;
        this.pagoMapper = pagoMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ListaPagosResponse listar(LocalDate desde, LocalDate hasta, String metodo, String estado) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : hoy;
        if (inicio.isAfter(fin)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        String codigoMetodo = (metodo == null || metodo.isBlank()) ? null : metodo.trim().toUpperCase();
        EstadoPago estadoPago = estadoDesde(estado);

        List<Pago> pagos = pagoRepository.buscarAdmin(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(),
                codigoMetodo, estadoPago);

        List<PagoAdminResponse> items = pagos.stream().map(pagoMapper::toAdmin).toList();
        BigDecimal totalAprobado = pagos.stream()
                .filter(p -> p.getEstado() == EstadoPago.APROBADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ListaPagosResponse(inicio.toString(), fin.toString(), items.size(), totalAprobado, items);
    }

    private EstadoPago estadoDesde(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPago.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Estado invalido. Usa APROBADO o RECHAZADO");
        }
    }
}