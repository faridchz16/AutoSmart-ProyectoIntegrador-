package com.tecsup.autosmart.reporte;

import com.tecsup.autosmart.dto.ListaPagosResponse;
import com.tecsup.autosmart.dto.PagoAdminResponse;
import com.tecsup.autosmart.service.AdminPagoService;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class PagosFuente implements FuenteReporte {

    private final AdminPagoService adminPagoService;

    public PagosFuente(AdminPagoService adminPagoService) {
        this.adminPagoService = adminPagoService;
    }

    @Override
    public String tipo() {
        return "pagos";
    }

    @Override
    public Reporte generar(Map<String, String> filtros) {
        ListaPagosResponse lista = adminPagoService.listar(
                FiltrosReporte.fecha(filtros, "desde"),
                FiltrosReporte.fecha(filtros, "hasta"),
                FiltrosReporte.texto(filtros, "metodo"),
                FiltrosReporte.texto(filtros, "estado"));

        List<List<Object>> filas = lista.pagos().stream()
                .map(this::fila)
                .toList();

        String periodo = FiltrosReporte.periodo(LocalDate.parse(lista.desde()), LocalDate.parse(lista.hasta()));
        return new Reporte(
                "Reporte de pagos del " + lista.desde() + " al " + lista.hasta(),
                "reporte-pagos-" + periodo,
                List.of("Código", "Fecha", "Ticket", "Cliente", "Placa", "Método", "Estado", "Monto (S/)", "Registrado por"),
                filas,
                List.of("Cantidad de pagos: " + lista.cantidad(),
                        "Total aprobado: S/ " + lista.totalAprobado().setScale(2, RoundingMode.HALF_UP).toPlainString()));
    }

    private List<Object> fila(PagoAdminResponse p) {
        return Arrays.asList(p.codigo(), p.fecha().replace('T', ' '), p.ticket(), p.cliente(), p.placa(),
                p.metodo(), p.estado(), p.monto(), p.registradoPor());
    }
}