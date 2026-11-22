package com.tecsup.autosmart.reporte;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Component
public class ExcelExportador implements ExportadorReporte {

    @Override
    public String formato() {
        return "xlsx";
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public byte[] exportar(Reporte reporte) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");
            CellStyle titulo = estiloNegrita(libro, 14, false);
            CellStyle cabecera = estiloNegrita(libro, 11, true);
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            int fila = 0;
            Cell celdaTitulo = hoja.createRow(fila++).createCell(0);
            celdaTitulo.setCellValue(reporte.titulo());
            celdaTitulo.setCellStyle(titulo);
            fila++;

            Row filaCabecera = hoja.createRow(fila++);
            for (int c = 0; c < reporte.columnas().size(); c++) {
                Cell celda = filaCabecera.createCell(c);
                celda.setCellValue(reporte.columnas().get(c));
                celda.setCellStyle(cabecera);
            }

            for (List<Object> datos : reporte.filas()) {
                Row row = hoja.createRow(fila++);
                for (int c = 0; c < datos.size(); c++) {
                    escribir(row.createCell(c), datos.get(c), moneda);
                }
            }

            if (!reporte.resumen().isEmpty()) {
                fila++;
                for (String linea : reporte.resumen()) {
                    hoja.createRow(fila++).createCell(0).setCellValue(linea);
                }
            }

            for (int c = 0; c < reporte.columnas().size(); c++) {
                hoja.autoSizeColumn(c);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    private void escribir(Cell celda, Object valor, CellStyle moneda) {
        if (valor == null) {
            celda.setCellValue("");
        } else if (valor instanceof BigDecimal monto) {
            celda.setCellValue(monto.doubleValue());
            celda.setCellStyle(moneda);
        } else if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
        } else {
            celda.setCellValue(valor.toString());
        }
    }

    private CellStyle estiloNegrita(Workbook libro, int tamano, boolean conFondo) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) tamano);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        if (conFondo) {
            estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return estilo;
    }
}