package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.AuthResponse;
import com.tecsup.autosmart.dto.LoginRequest;
import com.tecsup.autosmart.dto.RegisterRequest;
import com.tecsup.autosmart.model.Rol;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.RolRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import com.tecsup.autosmart.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> registrarUsuario(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }

        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseGet(() -> rolRepository.save(new Rol("CLIENTE")));

        String passwordHasheada = passwordEncoder.encode(request.getPassword());

        Usuario nuevoUsuario = new Usuario(
                request.getNombre(),
                request.getCorreo(),
                passwordHasheada,
                rolCliente
        );

        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Usuario registrado exitosamente");
        respuesta.put("idUsuario", guardado.getIdUsuario());
        respuesta.put("nombre", guardado.getNombre());
        respuesta.put("correo", guardado.getCorreo());
        respuesta.put("rol", guardado.getRol().getNombre());
        return respuesta;
    }

    public AuthResponse autenticarUsuario(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "CLIENTE";
        String token = jwtUtil.generarToken(usuario.getCorreo(), rolNombre);

        return new AuthResponse(token, usuario.getIdUsuario(), usuario.getNombre(), usuario.getCorreo(), rolNombre);
    }
}