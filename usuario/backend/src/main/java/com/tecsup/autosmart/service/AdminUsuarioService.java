package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.MecanicoCreadoResponse;
import com.tecsup.autosmart.dto.MecanicoRequest;
import com.tecsup.autosmart.dto.PaginaResponse;
import com.tecsup.autosmart.dto.UsuarioResumenResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.model.Rol;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.RolRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUsuarioService {

    private static final String MAYUS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> ROLES_VALIDOS = List.of("CLIENTE", "MECANICO", "ADMIN");

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

    // Lista paginada de usuarios, con filtro opcional por rol y busqueda por nombre o correo
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResumenResponse> listar(String rol, String q, int page, int size) {
        String rolFiltro = rol == null ? "" : rol.trim().toUpperCase();
        if (!rolFiltro.isEmpty() && !ROLES_VALIDOS.contains(rolFiltro)) {
            throw new BadRequestException("Rol invalido. Usa CLIENTE, MECANICO o ADMIN");
        }
        if (page < 0) {
            throw new BadRequestException("La pagina no puede ser negativa");
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException("El tamano debe estar entre 1 y 50");
        }
        String texto = q == null ? "" : q.trim();

        Page<Usuario> resultado = usuarioRepository.buscar(
                rolFiltro, texto, PageRequest.of(page, size, Sort.by("nombre").ascending()));

        PaginaResponse<UsuarioResumenResponse> res = new PaginaResponse<>();
        res.setContenido(resultado.getContent().stream()
                .map(this::mapearResumen)
                .collect(Collectors.toList()));
        res.setPagina(resultado.getNumber());
        res.setTamano(resultado.getSize());
        res.setTotalElementos(resultado.getTotalElements());
        res.setTotalPaginas(resultado.getTotalPages());
        return res;
    }

    private UsuarioResumenResponse mapearResumen(Usuario u) {
        UsuarioResumenResponse r = new UsuarioResumenResponse();
        r.setIdUsuario(u.getIdUsuario());
        r.setNombre(u.getNombre());
        r.setCorreo(u.getCorreo());
        r.setRol(u.getRol() != null ? u.getRol().getNombre() : null);
        return r;
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