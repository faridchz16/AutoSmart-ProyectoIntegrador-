package com.tecsup.autosmart.almacenamiento;

import com.tecsup.autosmart.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class AlmacenFotosDisco implements AlmacenFotos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenFotosDisco.class);

    private final Path carpeta;

    public AlmacenFotosDisco(@Value("${app.fotos.directorio}") String directorio) {
        this.carpeta = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
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

    @Override
    public byte[] leer(String nombreArchivo) {
        Path ruta = resolver(nombreArchivo);
        if (!Files.exists(ruta)) {
            throw new NotFoundException("El archivo de la foto no existe en el servidor");
        }
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto del servidor", e);
        }
    }

    @Override
    public void eliminar(String nombreArchivo) {
        try {
            Files.deleteIfExists(resolver(nombreArchivo));
        } catch (IOException | NotFoundException e) {
            log.warn("No se pudo borrar la foto {}: {}", nombreArchivo, e.getMessage());
        }
    }

    private Path resolver(String nombreArchivo) {
        Path ruta = carpeta.resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(carpeta)) {
            throw new NotFoundException("El archivo de la foto no existe en el servidor");
        }
        return ruta;
    }
}