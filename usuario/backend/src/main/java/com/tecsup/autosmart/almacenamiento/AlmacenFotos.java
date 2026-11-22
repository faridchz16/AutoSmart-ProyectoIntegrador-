package com.tecsup.autosmart.almacenamiento;

import org.springframework.web.multipart.MultipartFile;

public interface AlmacenFotos {

    String guardar(MultipartFile archivo, String extension);

    byte[] leer(String nombreArchivo);

    void eliminar(String nombreArchivo);
}