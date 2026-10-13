package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.ResetPasswordRequest;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.model.CodigoRecuperacion;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CodigoRecuperacionRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RecuperacionService {

    private static final int MINUTOS_VIGENCIA = 15;
    private static final int MAX_INTENTOS = 5;
    private static final String MSG_CODIGO_INVALIDO = "Codigo invalido o expirado";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacionRepository codigoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EnvioCorreoService envioCorreoService;

    public RecuperacionService(UsuarioRepository usuarioRepository,
                               CodigoRecuperacionRepository codigoRepository,
                               BCryptPasswordEncoder passwordEncoder,
                               EnvioCorreoService envioCorreoService) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioCorreoService = envioCorreoService;
    }

    // No revela si el correo existe: si no esta registrado o la cuenta esta desactivada, no hace nada
    @Transactional
    public void solicitarCodigo(String correo) {
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(correo.trim());
        if (encontrado.isEmpty() || Boolean.FALSE.equals(encontrado.get().getActivo())) {
            return;
        }
        Usuario usuario = encontrado.get();

        // Solo el codigo mas reciente es valido
        codigoRepository.findByUsuarioAndUsadoFalse(usuario).forEach(c -> c.setUsado(true));

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

        CodigoRecuperacion registro = new CodigoRecuperacion();
        registro.setUsuario(usuario);
        registro.setCodigoHash(passwordEncoder.encode(codigo));
        registro.setExpiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA));
        codigoRepository.save(registro);

        envioCorreoService.enviarCodigoRecuperacion(
                usuario.getCorreo(), usuario.getNombre(), codigo, MINUTOS_VIGENCIA);
    }

    // noRollbackFor: el conteo de intentos fallidos debe guardarse aunque se lance la excepcion
    @Transactional(noRollbackFor = BadRequestException.class)
    public void restablecer(ResetPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo().trim())
                .orElseThrow(() -> new BadRequestException(MSG_CODIGO_INVALIDO));

        CodigoRecuperacion registro = codigoRepository
                .findFirstByUsuarioAndUsadoFalseOrderByIdCodigoDesc(usuario)
                .orElseThrow(() -> new BadRequestException(MSG_CODIGO_INVALIDO));

        if (registro.getExpiraEn().isBefore(LocalDateTime.now())
                || registro.getIntentos() >= MAX_INTENTOS) {
            registro.setUsado(true);
            throw new BadRequestException(MSG_CODIGO_INVALIDO);
        }

        if (!passwordEncoder.matches(request.getCodigo(), registro.getCodigoHash())) {
            registro.setIntentos(registro.getIntentos() + 1);
            if (registro.getIntentos() >= MAX_INTENTOS) {
                registro.setUsado(true);
            }
            throw new BadRequestException(MSG_CODIGO_INVALIDO);
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        registro.setUsado(true);
    }
}