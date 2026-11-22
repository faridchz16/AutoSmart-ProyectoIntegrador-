package com.tecsup.autosmart.comprobante;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tecsup.autosmart.dto.CostoOrdenResponse;
import com.tecsup.autosmart.dto.CostoOrdenResponse.Adicional;
import com.tecsup.autosmart.dto.CostoOrdenResponse.RepuestoUtilizado;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.Pago;
import com.tecsup.autosmart.model.Vehiculo;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
public class OpenPdfComprobanteGenerator implements GeneradorComprobante {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color GRIS = new Color(230, 230, 230);

    private final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private final Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private final Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private final Font pequena = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

    @Override
    public byte[] generar(Pago pago, CostoOrdenResponse costo) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            encabezado(documento, pago);
            datosGenerales(documento, pago);
            detalle(documento, costo);
            repuestos(documento, costo);
            pie(documento);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private void encabezado(Document documento, Pago pago) throws DocumentException {
        Paragraph nombre = new Paragraph("AutoSmart - Taller Mecánico", titulo);
        nombre.setAlignment(Element.ALIGN_CENTER);
        documento.add(nombre);

        Paragraph tipo = new Paragraph("Comprobante de pago " + pago.getCodigo(), subtitulo);
        tipo.setAlignment(Element.ALIGN_CENTER);
        tipo.setSpacingAfter(15);
        documento.add(tipo);
    }

    private void datosGenerales(Document documento, Pago pago) throws DocumentException {
        Cita cita = pago.getOrden().getCita();
        Vehiculo vehiculo = cita.getVehiculo();

        PdfPTable tabla = new PdfPTable(new float[]{1.2f, 2f, 1.2f, 2f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(15);

        dato(tabla, "Fecha de pago", pago.getFecha().format(FORMATO_FECHA));
        dato(tabla, "Ticket", cita.getTicket());
        dato(tabla, "Cliente", cita.getCliente() != null ? cita.getCliente().getNombre() : "-");
        dato(tabla, "Método", pago.getMetodo().getNombre());
        dato(tabla, "Vehículo", vehiculo != null ? vehiculo.getMarca() + " " + vehiculo.getModelo() : "-");
        dato(tabla, "Placa", vehiculo != null ? vehiculo.getPlaca() : "-");
        dato(tabla, "Mecánico", cita.getMecanico() != null ? cita.getMecanico().getNombre() : "-");
        dato(tabla, "Estado", pago.getEstado().name());

        documento.add(tabla);
    }

    private void detalle(Document documento, CostoOrdenResponse costo) throws DocumentException {
        documento.add(new Paragraph("Detalle del servicio", subtitulo));

        PdfPTable tabla = new PdfPTable(new float[]{4f, 1f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(5);
        tabla.setSpacingAfter(10);

        cabecera(tabla, "Concepto");
        cabecera(tabla, "Cant.");
        cabecera(tabla, "P. unit.");
        cabecera(tabla, "Subtotal");

        BigDecimal precioServicio = costo.servicio().precio();
        fila(tabla, costo.servicio().nombre(), "1", soles(precioServicio), soles(precioServicio));

        for (Adicional adicional : costo.adicionales()) {
            fila(tabla, adicional.tipo() + ": " + adicional.descripcion(),
                    String.valueOf(adicional.cantidad()),
                    soles(adicional.precioUnitario()),
                    soles(adicional.subtotal()));
        }

        PdfPCell etiquetaTotal = new PdfPCell(new Phrase("TOTAL PAGADO", negrita));
        etiquetaTotal.setColspan(3);
        etiquetaTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        etiquetaTotal.setBackgroundColor(GRIS);
        etiquetaTotal.setPadding(5);
        tabla.addCell(etiquetaTotal);

        PdfPCell total = new PdfPCell(new Phrase(soles(costo.total()), negrita));
        total.setHorizontalAlignment(Element.ALIGN_RIGHT);
        total.setBackgroundColor(GRIS);
        total.setPadding(5);
        tabla.addCell(total);

        documento.add(tabla);
    }

    private void repuestos(Document documento, CostoOrdenResponse costo) throws DocumentException {
        if (costo.repuestosUtilizados().isEmpty()) {
            return;
        }
        documento.add(new Paragraph("Repuestos y materiales utilizados", subtitulo));
        for (RepuestoUtilizado repuesto : costo.repuestosUtilizados()) {
            String unidad = repuesto.unidad() != null ? " " + repuesto.unidad() : "";
            documento.add(new Paragraph("- " + repuesto.material() + ": " + repuesto.cantidad() + unidad, normal));
        }
    }

    private void pie(Document documento) throws DocumentException {
        Paragraph pie = new Paragraph();
        pie.setSpacingBefore(25);
        pie.add(new Chunk("Gracias por confiar en AutoSmart. ", normal));
        pie.add(new Chunk("Documento no tributario generado por el sistema.", pequena));
        pie.setAlignment(Element.ALIGN_CENTER);
        documento.add(pie);
    }

    private void dato(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, negrita));
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        celdaEtiqueta.setPadding(3);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "-", normal));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setPadding(3);
        tabla.addCell(celdaValor);
    }

    private void cabecera(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, negrita));
        celda.setBackgroundColor(GRIS);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private void fila(PdfPTable tabla, String concepto, String cantidad, String unitario, String subtotal) {
        tabla.addCell(celda(concepto, Element.ALIGN_LEFT));
        tabla.addCell(celda(cantidad, Element.ALIGN_CENTER));
        tabla.addCell(celda(unitario, Element.ALIGN_RIGHT));
        tabla.addCell(celda(subtotal, Element.ALIGN_RIGHT));
    }

    private PdfPCell celda(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, normal));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5);
        return celda;
    }

    private String soles(BigDecimal monto) {
        BigDecimal valor = monto != null ? monto : BigDecimal.ZERO;
        return "S/ " + valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}