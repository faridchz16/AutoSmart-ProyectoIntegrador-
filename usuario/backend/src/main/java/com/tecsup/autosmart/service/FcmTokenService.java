package com.tecsup.autosmart.service;

import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FcmTokenService {

    private final UsuarioRepository usuarioRepository;

    public FcmTokenService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(String correo, String token) {
        Usuario usuario = buscar(correo);
        usuario.setFcmToken(token.trim());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(String correo) {
        Usuario usuario = buscar(correo);
        usuario.setFcmToken(null);
        usuarioRepository.save(usuario);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidar(String token) {
        usuarioRepository.findByFcmToken(token).ifPresent(u -> {
            u.setFcmToken(null);
            usuarioRepository.save(u);
        });
    }

    private Usuario buscar(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}