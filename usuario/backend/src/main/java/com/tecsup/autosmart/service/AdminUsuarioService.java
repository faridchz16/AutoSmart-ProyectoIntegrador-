package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.MecanicoCreadoResponse;
import com.tecsup.autosmart.dto.MecanicoRequest;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.model.Rol;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.RolRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class AdminUsuarioService {

    private static final String MAYUS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminUsuarioService(UsuarioRepository usuarioRepository,
                               RolRepository rolRepository,
                               BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MecanicoCreadoResponse crearMecanico(MecanicoRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictException("El correo ya se encuentra registrado");
        }

        Rol rolMecanico = rolRepository.findByNombre("MECANICO")
                .orElseGet(() -> rolRepository.save(new Rol("MECANICO")));

        String passwordTemporal = generarPasswordTemporal();

        Usuario mecanico = new Usuario(
                request.getNombre().trim(),
                correo,
                passwordEncoder.encode(passwordTemporal),
                rolMecanico
        );
        Usuario guardado = usuarioRepository.save(mecanico);

        MecanicoCreadoResponse res = new MecanicoCreadoResponse();
        res.setIdUsuario(guardado.getIdUsuario());
        res.setNombre(guardado.getNombre());
        res.setCorreo(guardado.getCorreo());
        res.setRol(guardado.getRol().getNombre());
        res.setPasswordTemporal(passwordTemporal);
        return res;
    }

    // 10 caracteres con al menos una mayuscula, una minuscula y un digito
    private String generarPasswordTemporal() {
        String todos = MAYUS + MINUS + DIGITOS;
        StringBuilder sb = new StringBuilder();
        sb.append(MAYUS.charAt(RANDOM.nextInt(MAYUS.length())));
        sb.append(MINUS.charAt(RANDOM.nextInt(MINUS.length())));
        sb.append(DIGITOS.charAt(RANDOM.nextInt(DIGITOS.length())));
        for (int i = 3; i < 10; i++) {
            sb.append(todos.charAt(RANDOM.nextInt(todos.length())));
        }
        return sb.toString();
    }
}