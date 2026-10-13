package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.PerfilMecanicoRequest;
import com.tecsup.autosmart.dto.PerfilMecanicoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Especialidad;
import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.EspecialidadRepository;
import com.tecsup.autosmart.repository.PerfilMecanicoRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MecanicoService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilMecanicoRepository perfilRepository;
    private final EspecialidadRepository especialidadRepository;

    public MecanicoService(UsuarioRepository usuarioRepository,
                           PerfilMecanicoRepository perfilRepository,
                           EspecialidadRepository especialidadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.especialidadRepository = especialidadRepository;
    }

    // Si el mecanico aun no completo su perfil, devuelve uno vacio (0 anios, sin especialidades)
    @Transactional(readOnly = true)
    public PerfilMecanicoResponse obtenerPerfil(String correo) {
        Usuario usuario = buscarUsuario(correo);
        PerfilMecanico perfil = perfilRepository.findByUsuario(usuario).orElse(null);
        return mapear(usuario, perfil);
    }

    @Transactional
    public PerfilMecanicoResponse guardarPerfil(String correo, PerfilMecanicoRequest request) {
        Usuario usuario = buscarUsuario(correo);

        PerfilMecanico perfil = perfilRepository.findByUsuario(usuario).orElseGet(() -> {
            PerfilMecanico nuevo = new PerfilMecanico();
            nuevo.setUsuario(usuario);
            return nuevo;
        });

        perfil.setAniosExperiencia(request.getAniosExperiencia());
        perfil.setEspecialidades(resolverEspecialidades(request.getEspecialidades()));

        return mapear(usuario, perfilRepository.save(perfil));
    }

    // Normaliza los nombres, descarta repetidos y crea las especialidades que aun no existen
    private Set<Especialidad> resolverEspecialidades(List<String> nombres) {
        Set<String> limpios = new HashSet<>();
        for (String n : nombres) {
            if (n == null || n.isBlank()) {
                throw new BadRequestException("Las especialidades no pueden estar vacias");
            }
            String nombre = n.trim();
            if (nombre.length() > 60) {
                throw new BadRequestException("Cada especialidad puede tener hasta 60 caracteres");
            }
            limpios.add(nombre.toLowerCase());
        }

        Set<Especialidad> resultado = new HashSet<>();
        for (String nombre : limpios) {
            Especialidad esp = especialidadRepository.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> especialidadRepository.save(new Especialidad(capitalizar(nombre))));
            resultado.add(esp);
        }
        return resultado;
    }

    private String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private PerfilMecanicoResponse mapear(Usuario usuario, PerfilMecanico perfil) {
        PerfilMecanicoResponse res = new PerfilMecanicoResponse();
        res.setIdUsuario(usuario.getIdUsuario());
        res.setNombre(usuario.getNombre());
        res.setCorreo(usuario.getCorreo());
        if (perfil == null) {
            res.setAniosExperiencia(0);
            res.setEspecialidades(List.of());
        } else {
            res.setAniosExperiencia(perfil.getAniosExperiencia());
            res.setEspecialidades(perfil.getEspecialidades().stream()
                    .map(Especialidad::getNombre)
                    .sorted()
                    .collect(Collectors.toList()));
        }
        return res;
    }
}