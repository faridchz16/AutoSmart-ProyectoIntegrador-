package com.tecsup.autosmart.reporte;

import com.tecsup.autosmart.dto.ListaPagosResponse;
import com.tecsup.autosmart.dto.PagoAdminResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.service.AdminPagoService;
import com.tecsup.autosmart.service.ReporteService;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportesTest {

    private Reporte reporte;
    private ReporteService reporteService;

    @BeforeEach
    void preparar() {
        reporte = new Reporte("Reporte de pagos", "reporte-pagos-2026-11",
                List.of("Código", "Cliente", "Monto (S/)"),
                List.of(Arrays.asList("PAG-2026-00006", "Cliente Prueba", new BigDecimal("150.00")),
                        Arrays.asList("PAG-2026-00007", null, new BigDecimal("75.50"))),
                List.of("Total aprobado: S/ 225.50"));

        AdminPagoService adminPagoService = mock(AdminPagoService.class);
        when(adminPagoService.listar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "TARJETA", null))
                .thenReturn(new ListaPagosResponse("2026-11-01", "2026-11-30", 1, new BigDecimal("150.00"),
                        List.of(new PagoAdminResponse(6L, "PAG-2026-00006", "2026-11-21T23:45:00", "AS-2026-00035",
                                "Cliente Prueba", "DEF-456", "TARJETA", "APROBADO", new BigDecimal("150.00"), null))));

        reporteService = new ReporteService(List.of(new PagosFuente(adminPagoService)),
                List.of(new ExcelExportador(), new PdfExportador()));
    }

    @Test
    void elExcelTieneTituloCabeceraFilasYMontosNumericos() throws Exception {
        byte[] archivo = new ExcelExportador().exportar(reporte);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("Reporte de pagos", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Código", hoja.getRow(2).getCell(0).getStringCellValue());
            assertEquals("PAG-2026-00006", hoja.getRow(3).getCell(0).getStringCellValue());
            assertEquals(150.00, hoja.getRow(3).getCell(2).getNumericCellValue());
            assertEquals("", hoja.getRow(4).getCell(1).getStringCellValue());
            assertEquals("Total aprobado: S/ 225.50", hoja.getRow(6).getCell(0).getStringCellValue());
        }
    }

    @Test
    void elPdfEsValidoAunqueNoHayaFilas() {
        Reporte vacio = new Reporte("Sin datos", "vacio", List.of("A", "B"), List.of(), List.of());

        byte[] archivo = new PdfExportador().exportar(vacio);

        assertEquals("%PDF", new String(archivo, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void generaElReporteDePagosConNombreYTipoDeArchivo() {
        ArchivoReporte xlsx = reporteService.generar("PAGOS", null,
                Map.of("desde", "2026-11-01", "hasta", "2026-11-30", "metodo", "TARJETA"));
        ArchivoReporte pdf = reporteService.generar("pagos", "pdf",
                Map.of("desde", "2026-11-01", "hasta", "2026-11-30", "metodo", "TARJETA"));

        assertEquals("reporte-pagos-2026-11.xlsx", xlsx.nombreArchivo());
        assertTrue(xlsx.contentType().contains("spreadsheetml"));
        assertEquals("reporte-pagos-2026-11.pdf", pdf.nombreArchivo());
        assertEquals("application/pdf", pdf.contentType());
    }

    @Test
    void rechazaTipoFormatoYFechasInvalidos() {
        assertThrows(BadRequestException.class, () -> reporteService.generar("ventas", "xlsx", Map.of()));
        assertThrows(BadRequestException.class, () -> reporteService.generar("pagos", "docx", Map.of()));
        assertThrows(BadRequestException.class, () -> reporteService.generar("pagos", "pdf", Map.of("desde", "21/11/2026")));
    }

    @Test
    void elPeriodoUsaElMesCuandoElRangoEsUnMesCompleto() {
        assertEquals("2026-11", FiltrosReporte.periodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
        assertEquals("2026-11-05_2026-11-20", FiltrosReporte.periodo(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 20)));
    }
}