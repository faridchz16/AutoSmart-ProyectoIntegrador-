package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.model.Derivacion;
import com.tecsup.autosmart.model.EmisorChat;
import com.tecsup.autosmart.model.MensajeChatbot;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.service.DerivacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HablarAsesorManejadorTest {

    private static final String CONVERSACION = "c7f1e2a0-5b1d-4c2e-9f00-1a2b3c4d5e6f";

    private DerivacionService derivacionService;
    private HablarAsesorManejador manejador;

    @BeforeEach
    void preparar() {
        derivacionService = mock(DerivacionService.class);
        manejador = new HablarAsesorManejador(derivacionService);
        when(derivacionService.pendienteDe(anyString())).thenReturn(Optional.empty());
        when(derivacionService.ultimosMensajes(anyString())).thenReturn(List.of());
        when(derivacionService.registrar(anyString(), any(), any(), anyString())).thenReturn(derivacion(5L));
    }

    @Test
    void detectaElContactoEnElTexto() {
        assertEquals("cliente@mail.com", HablarAsesorManejador.contacto("mi correo es Cliente@Mail.com"));
        assertEquals("987654321", HablarAsesorManejador.contacto("llamame al +51 987 654 321"));
        assertNull(HablarAsesorManejador.contacto("no tengo"));
    }

    @Test
    void sinSesionNiContactoPideUnTelefonoOCorreo() {
        MensajeChat mensaje = MensajeChat.de(CONVERSACION, "Quiero hablar con un asesor", null);

        assertTrue(manejador.aplica(mensaje));
        RespuestaChat res = manejador.responder(mensaje);

        assertTrue(res.respuesta().contains("teléfono"));
        assertNull(res.datos());
        verify(derivacionService, never()).registrar(anyString(), any(), any(), anyString());
    }

    @Test
    void registraCuandoElVisitanteEnviaSuContactoDespuesDeQueSeLoPidieron() {
        MensajeChatbot pedido = new MensajeChatbot(CONVERSACION, null, EmisorChat.BOT, "Escríbeme tu teléfono",
                HablarAsesorManejador.INTENCION, LocalDateTime.now());
        when(derivacionService.ultimosMensajes(CONVERSACION)).thenReturn(List.of(pedido));
        MensajeChat mensaje = MensajeChat.de(CONVERSACION, "987654321", null);

        assertTrue(manejador.aplica(mensaje));
        RespuestaChat res = manejador.responder(mensaje);

        assertEquals(Map.of("idDerivacion", 5L), res.datos());
        verify(derivacionService).registrar(CONVERSACION, null, "987654321", "987654321");
    }

    @Test
    void unNumeroSueltoSinHaberPedidoAsesorNoSeConsideraDerivacion() {
        assertFalse(manejador.aplica(MensajeChat.de(CONVERSACION, "987654321", null)));
    }

    @Test
    void conSesionRegistraDirectamente() {
        Usuario cliente = new Usuario();
        cliente.setCorreo("cliente2@mail.com");

        RespuestaChat res = manejador.responder(MensajeChat.de(CONVERSACION, "quiero un asesor", cliente));

        assertEquals(Map.of("idDerivacion", 5L), res.datos());
        verify(derivacionService).registrar(eq(CONVERSACION), eq(cliente), isNull(), anyString());
    }

    @Test
    void noDuplicaSiYaHayUnaDerivacionPendiente() {
        when(derivacionService.pendienteDe(CONVERSACION)).thenReturn(Optional.of(derivacion(3L)));

        RespuestaChat res = manejador.responder(MensajeChat.de(CONVERSACION, "asesor", null));

        assertTrue(res.respuesta().startsWith("Ya registramos"));
        verify(derivacionService, never()).registrar(anyString(), any(), any(), anyString());
    }

    private Derivacion derivacion(Long id) {
        Derivacion d = new Derivacion();
        d.setIdDerivacion(id);
        return d;
    }
}