package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.FotoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Deficiencia;
import com.tecsup.autosmart.model.FotoDeficiencia;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.DeficienciaRepository;
import com.tecsup.autosmart.repository.FotoDeficienciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class FotoDeficienciaService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final int MAXIMO_FOTOS = 5;

    private final FotoDeficienciaRepository fotoRepository;
    private final DeficienciaRepository deficienciaRepository;
    private final OrdenTrabajoService ordenService;
    private final AlmacenFotosService almacen;

    public FotoDeficienciaService(FotoDeficienciaRepository fotoRepository,
                                  DeficienciaRepository deficienciaRepository,
                                  OrdenTrabajoService ordenService,
                                  AlmacenFotosService almacen) {
        this.fotoRepository = fotoRepository;
        this.deficienciaRepository = deficienciaRepository;
        this.ordenService = ordenService;
        this.almacen = almacen;
    }

    @Transactional
    public FotoResponse subir(String correoMecanico, Long idOrden, Long idDeficiencia, MultipartFile archivo) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        Deficiencia deficiencia = deficienciaRepository.findById(idDeficiencia)
                .filter(d -> d.getOrden().getIdOrden().equals(idOrden))
                .orElseThrow(() -> new NotFoundException("Deficiencia no encontrada en esta orden"));

        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debes enviar la foto en el campo 'foto'");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new BadRequestException("La foto no puede superar 5 MB");
        }
        String tipo = detectarTipo(archivo);
        if (fotoRepository.countByDeficiencia_IdDeficiencia(idDeficiencia) >= MAXIMO_FOTOS) {
            throw new ConflictException("La deficiencia ya tiene el maximo de " + MAXIMO_FOTOS + " fotos");
        }

        String extension = tipo.equals("image/png") ? "png" : "jpg";
        String nombreArchivo = almacen.guardar(archivo, extension);
        try {
            FotoDeficiencia foto = new FotoDeficiencia();
            foto.setDeficiencia(deficiencia);
            foto.setNombreArchivo(nombreArchivo);
            foto.setTipoContenido(tipo);
            foto.setTamanoBytes(archivo.getSize());
            foto.setFechaRegistro(LocalDateTime.now(ZONA_LIMA).withNano(0));
            FotoDeficiencia guardada = fotoRepository.save(foto);
            return new FotoResponse(guardada.getIdFoto(), idDeficiencia, "/photos/" + guardada.getIdFoto());
        } catch (RuntimeException e) {
            almacen.eliminar(nombreArchivo);
            throw e;
        }
    }

    private String detectarTipo(MultipartFile archivo) {
        byte[] cabecera = new byte[8];
        int leidos;
        try (InputStream entrada = archivo.getInputStream()) {
            leidos = entrada.readNBytes(cabecera, 0, cabecera.length);
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer la foto");
        }
        boolean esJpg = leidos >= 3
                && (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xFF) == 0xD8 && (cabecera[2] & 0xFF) == 0xFF;
        boolean esPng = leidos >= 8
                && (cabecera[0] & 0xFF) == 0x89 && cabecera[1] == 'P' && cabecera[2] == 'N' && cabecera[3] == 'G';
        if (esJpg) return "image/jpeg";
        if (esPng) return "image/png";
        throw new BadRequestException("Solo se permiten fotos JPG o PNG");
    }
}