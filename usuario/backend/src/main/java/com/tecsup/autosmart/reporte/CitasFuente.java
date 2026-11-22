package com.tecsup.autosmart.reporte;

import com.tecsup.autosmart.dto.CitaAdminResponse;
import com.tecsup.autosmart.dto.PaginaResponse;
import com.tecsup.autosmart.service.AdminCitaService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class CitasFuente implements FuenteReporte {

    private static final int TAMANO_PAGINA = 50;
    private static final int MAX_PAGINAS = 100;

    private final AdminCitaService adminCitaService;
    private final Clock clock;

    public CitasFuente(AdminCitaService adminCitaService, Clock clock) {
        this.adminCitaService = adminCitaService;
        this.clock = clock;
    }

    @Override
    public String tipo() {
        return "citas";
    }

    @Override
    public Reporte generar(Map<String, String> filtros) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate desde = FiltrosReporte.fecha(filtros, "desde");
        LocalDate hasta = FiltrosReporte.fecha(filtros, "hasta");
        LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : hoy.withDayOfMonth(hoy.lengthOfMonth());
        String estado = FiltrosReporte.texto(filtros, "estado");

        List<CitaAdminResponse> citas = new ArrayList<>();
        int pagina = 0;
        PaginaResponse<CitaAdminResponse> resultado;
        do {
            resultado = adminCitaService.listar(inicio, fin, estado, null, null, pagina, TAMANO_PAGINA);
            citas.addAll(resultado.getContenido());
            pagina++;
        } while (pagina < resultado.getTotalPaginas() && pagina < MAX_PAGINAS);

        List<List<Object>> filas = citas.stream()
                .map(c -> Arrays.<Object>asList(c.getTicket(), c.getFecha(), c.getHoraInicio(), c.getEstado(),
                        c.getServicio(), c.getCliente(), c.getPlaca(), c.getMecanico(), c.getPrecio()))
                .toList();

        return new Reporte(
                "Reporte de citas del " + inicio + " al " + fin + (estado == null ? "" : " (" + estado.toUpperCase() + ")"),
                "reporte-citas-" + FiltrosReporte.periodo(inicio, fin),
                List.of("Ticket", "Fecha", "Hora", "Estado", "Servicio", "Cliente", "Placa", "Mecánico", "Precio (S/)"),
                filas,
                List.of("Cantidad de citas: " + citas.size()));
    }
}