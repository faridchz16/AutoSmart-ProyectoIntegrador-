package com.tecsup.autosmart.almacenamiento;

import com.tecsup.autosmart.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenFotosDiscoTest {

    @TempDir
    Path carpeta;

    @Test
    void guardaLeeYEliminaUnaFoto() {
        AlmacenFotos almacen = new AlmacenFotosDisco(carpeta.toString());
        byte[] contenido = {1, 2, 3, 4};

        String nombre = almacen.guardar(new MockMultipartFile("foto", "auto.jpg", "image/jpeg", contenido), "jpg");

        assertTrue(nombre.endsWith(".jpg"));
        assertArrayEquals(contenido, almacen.leer(nombre));
        almacen.eliminar(nombre);
        assertFalse(Files.exists(carpeta.resolve(nombre)));
    }

    @Test
    void noPermiteSalirDeLaCarpetaNiLeerArchivosInexistentes() {
        AlmacenFotos almacen = new AlmacenFotosDisco(carpeta.toString());

        assertThrows(NotFoundException.class, () -> almacen.leer("../../application.properties"));
        assertThrows(NotFoundException.class, () -> almacen.leer("no-existe.jpg"));
        assertDoesNotThrow(() -> almacen.eliminar("../fuera.jpg"));
    }
}