package com.tecsup.autosmart.service;

import com.tecsup.autosmart.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class AlmacenFotosService {

    private final Path carpeta;

    public AlmacenFotosService(@Value("${app.fotos.directorio}") String directorio) {
        this.carpeta = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo, String extension) {
        String nombre = UUID.randomUUID() + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(carpeta);
            Files.copy(entrada, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            return nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto en el servidor", e);
        }
    }

    public byte[] leer(String nombreArchivo) {
        Path ruta = carpeta.resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(carpeta) || !Files.exists(ruta)) {
            throw new NotFoundException("El archivo de la foto no existe en el servidor");
        }
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto del servidor", e);
        }
    }

    public void eliminar(String nombreArchivo) {
        try {
            Files.deleteIfExists(carpeta.resolve(nombreArchivo).normalize());
        } catch (IOException ignored) {
            // Si no se puede borrar, solo queda un archivo huerfano en la carpeta
        }
    }
}