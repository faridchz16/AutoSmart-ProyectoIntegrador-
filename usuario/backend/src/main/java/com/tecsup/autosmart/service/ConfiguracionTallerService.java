package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.DiaTallerDto;
import com.tecsup.autosmart.dto.FeriadoRequest;
import com.tecsup.autosmart.dto.FeriadoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.ConfiguracionTaller;
import com.tecsup.autosmart.model.Feriado;
import com.tecsup.autosmart.repository.ConfiguracionTallerRepository;
import com.tecsup.autosmart.repository.FeriadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfiguracionTallerService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ConfiguracionTallerRepository configuracionRepository;
    private final FeriadoRepository feriadoRepository;

    public ConfiguracionTallerService(ConfiguracionTallerRepository configuracionRepository,
                                      FeriadoRepository feriadoRepository) {
        this.configuracionRepository = configuracionRepository;
        this.feriadoRepository = feriadoRepository;
    }

    // ---------- Horario del taller ----------

    @Transactional(readOnly = true)
    public List<DiaTallerDto> listarHorario() {
        List<DiaTallerDto> dias = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            dias.add(aDto(obtenerDia(dia)));
        }
        return dias;
    }

    // Si el administrador aun no configuro ese dia, se usa el horario por defecto (no se guarda)
    @Transactional(readOnly = true)
    public ConfiguracionTaller obtenerDia(int dia) {
        return configuracionRepository.findById(dia).orElseGet(() -> porDefecto(dia));
    }

    @Transactional
    public List<DiaTallerDto> actualizarHorario(List<DiaTallerDto> dias) {
        if (dias == null || dias.size() != 7) {
            throw new BadRequestException("Debes enviar los 7 dias de la semana");
        }

        Set<Integer> vistos = new HashSet<>();
        List<ConfiguracionTaller> nuevos = new ArrayList<>();

        for (DiaTallerDto d : dias) {
            if (d == null || d.getDiaSemana() == null || d.getDiaSemana() < 1 || d.getDiaSemana() > 7) {
                throw new BadRequestException("diaSemana debe estar entre 1 y 7");
            }
            if (!vistos.add(d.getDiaSemana())) {
                throw new BadRequestException("El dia " + d.getDiaSemana() + " esta repetido");
            }
            if (d.getAbierto() == null) {
                throw new BadRequestException("'abierto' es obligatorio (dia " + d.getDiaSemana() + ")");
            }
            LocalTime apertura = parsearHora(d.getHoraApertura(), d.getDiaSemana());
            LocalTime cierre = parsearHora(d.getHoraCierre(), d.getDiaSemana());
            if (d.getAbierto() && !apertura.isBefore(cierre)) {
                throw new BadRequestException(
                        "La hora de apertura debe ser anterior a la de cierre (dia " + d.getDiaSemana() + ")");
            }
            nuevos.add(new ConfiguracionTaller(d.getDiaSemana(), d.getAbierto(), apertura, cierre));
        }

        configuracionRepository.saveAll(nuevos);
        return listarHorario();
    }

    // ---------- Feriados ----------

    @Transactional(readOnly = true)
    public List<FeriadoResponse> listarFeriados(Integer anio) {
        int a = anio == null ? LocalDate.now(LIMA).getYear() : anio;
        if (a < 2000 || a > 2100) {
            throw new BadRequestException("El anio debe estar entre 2000 y 2100");
        }
        return feriadoRepository
                .findByFechaBetweenOrderByFechaAsc(LocalDate.of(a, 1, 1), LocalDate.of(a, 12, 31))
                .stream().map(this::aResponse).collect(Collectors.toList());
    }

    @Transactional
    public FeriadoResponse crearFeriado(FeriadoRequest request) {
        if (feriadoRepository.existsByFecha(request.getFecha())) {
            throw new ConflictException("Ya existe un feriado en esa fecha");
        }
        Feriado feriado = new Feriado(request.getFecha(), request.getDescripcion().trim());
        return aResponse(feriadoRepository.save(feriado));
    }

    @Transactional
    public void eliminarFeriado(Integer id) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Feriado no encontrado"));
        feriadoRepository.delete(feriado);
    }

    @Transactional(readOnly = true)
    public boolean esFeriado(LocalDate fecha) {
        return feriadoRepository.existsByFecha(fecha);
    }

    // ---------- Auxiliares ----------

    private ConfiguracionTaller porDefecto(int dia) {
        if (dia <= 5) {
            return new ConfiguracionTaller(dia, true, LocalTime.of(8, 0), LocalTime.of(18, 0));
        }
        if (dia == 6) {
            return new ConfiguracionTaller(dia, true, LocalTime.of(8, 0), LocalTime.of(13, 0));
        }
        return new ConfiguracionTaller(dia, false, LocalTime.of(8, 0), LocalTime.of(18, 0));
    }

    private LocalTime parsearHora(String texto, int dia) {
        if (texto == null || texto.isBlank()) {
            throw new BadRequestException("La hora es obligatoria (dia " + dia + ")");
        }
        try {
            return LocalTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Hora invalida '" + texto + "' (dia " + dia + "). Usa HH:mm");
        }
    }

    private DiaTallerDto aDto(ConfiguracionTaller c) {
        DiaTallerDto dto = new DiaTallerDto();
        dto.setDiaSemana(c.getDiaSemana());
        dto.setAbierto(c.getAbierto());
        dto.setHoraApertura(c.getHoraApertura().toString());
        dto.setHoraCierre(c.getHoraCierre().toString());
        return dto;
    }

    private FeriadoResponse aResponse(Feriado f) {
        FeriadoResponse res = new FeriadoResponse();
        res.setIdFeriado(f.getIdFeriado());
        res.setFecha(f.getFecha().toString());
        res.setDescripcion(f.getDescripcion());
        return res;
    }
}