package com.tecsup.autosmart.reporte;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PdfExportador implements ExportadorReporte {

    private static final Color GRIS = new Color(230, 230, 230);

    @Override
    public String formato() {
        return "pdf";
    }

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public byte[] exportar(Reporte reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph encabezado = new Paragraph("AutoSmart - " + reporte.titulo(), titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            encabezado.setSpacingAfter(12);
            documento.add(encabezado);

            PdfPTable tabla = new PdfPTable(reporte.columnas().size());
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String columna : reporte.columnas()) {
                PdfPCell celda = new PdfPCell(new Phrase(columna, negrita));
                celda.setBackgroundColor(GRIS);
                celda.setPadding(4);
                tabla.addCell(celda);
            }
            for (List<Object> fila : reporte.filas()) {
                for (Object valor : fila) {
                    PdfPCell celda = new PdfPCell(new Phrase(texto(valor), normal));
                    celda.setPadding(4);
                    if (valor instanceof Number) {
                        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    tabla.addCell(celda);
                }
            }
            if (reporte.filas().isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("Sin registros para los filtros seleccionados", normal));
                vacio.setColspan(reporte.columnas().size());
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                vacio.setPadding(6);
                tabla.addCell(vacio);
            }
            documento.add(tabla);

            for (String linea : reporte.resumen()) {
                Paragraph p = new Paragraph(linea, negrita);
                p.setSpacingBefore(4);
                documento.add(p);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el archivo PDF", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private String texto(Object valor) {
        if (valor == null) {
            return "-";
        }
        if (valor instanceof BigDecimal monto) {
            return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return valor.toString();
    }
}