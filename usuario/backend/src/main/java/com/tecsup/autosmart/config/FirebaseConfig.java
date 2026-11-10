package com.tecsup.autosmart.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    public FirebaseConfig(@Value("${app.firebase.credenciales:}") String rutaCredenciales) {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        if (rutaCredenciales == null || rutaCredenciales.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS no esta configurada: las notificaciones push estan desactivadas");
            return;
        }
        Path ruta = Path.of(rutaCredenciales);
        if (!Files.exists(ruta)) {
            log.warn("No se encontro la credencial de Firebase en {}: las notificaciones push estan desactivadas", ruta);
            return;
        }
        try (InputStream entrada = new FileInputStream(ruta.toFile())) {
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(entrada))
                    .build();
            FirebaseApp.initializeApp(opciones);
            log.info("Firebase inicializado: notificaciones push activas");
        } catch (Exception e) {
            log.error("No se pudo inicializar Firebase: las notificaciones push estan desactivadas", e);
        }
    }
}