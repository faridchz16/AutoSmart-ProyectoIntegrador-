package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.PerfilRequest;
import com.tecsup.autosmart.dto.PerfilResponse;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(String correo) {
        return mapear(buscar(correo));
    }

    @Transactional
    public PerfilResponse actualizarPerfil(String correo, PerfilRequest request) {
        Usuario usuario = buscar(correo);

        usuario.setNombre(request.getNombre().trim());

        // Un telefono vacio se guarda como null (el usuario lo quita)
        String telefono = request.getTelefono() == null ? "" : request.getTelefono().trim();
        usuario.setTelefono(telefono.isEmpty() ? null : telefono);

        return mapear(usuarioRepository.save(usuario));
    }

    private Usuario buscar(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private PerfilResponse mapear(Usuario u) {
        PerfilResponse res = new PerfilResponse();
        res.setIdUsuario(u.getIdUsuario());
        res.setNombre(u.getNombre());
        res.setCorreo(u.getCorreo());
        res.setTelefono(u.getTelefono());
        res.setRol(u.getRol() != null ? u.getRol().getNombre() : null);
        return res;
    }
}