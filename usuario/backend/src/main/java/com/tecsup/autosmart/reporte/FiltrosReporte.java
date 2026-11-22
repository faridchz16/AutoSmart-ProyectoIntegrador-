package com.tecsup.autosmart.reporte;

import com.tecsup.autosmart.exception.BadRequestException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class FiltrosReporte {

    private FiltrosReporte() {
    }

    public static String texto(Map<String, String> filtros, String clave) {
        String valor = filtros.get(clave);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public static LocalDate fecha(Map<String, String> filtros, String clave) {
        String valor = texto(filtros, clave);
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("El parametro '" + clave + "' debe tener el formato yyyy-MM-dd");
        }
    }

    public static YearMonth mes(Map<String, String> filtros, String clave) {
        String valor = texto(filtros, clave);
        if (valor == null) {
            return null;
        }
        try {
            return YearMonth.parse(valor);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("El parametro '" + clave + "' debe tener el formato yyyy-MM");
        }
    }

    public static String periodo(LocalDate desde, LocalDate hasta) {
        YearMonth mes = YearMonth.from(desde);
        if (desde.equals(mes.atDay(1)) && YearMonth.from(hasta).equals(mes)) {
            return mes.toString();
        }
        return desde + "_" + hasta;
    }
}