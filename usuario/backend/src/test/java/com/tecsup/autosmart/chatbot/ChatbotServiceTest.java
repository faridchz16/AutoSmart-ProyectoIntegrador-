package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.ChatbotRequest;
import com.tecsup.autosmart.dto.ChatbotResponse;
import com.tecsup.autosmart.dto.DiaTallerDto;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.model.Faq;
import com.tecsup.autosmart.model.MensajeChatbot;
import com.tecsup.autosmart.repository.MensajeChatbotRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import com.tecsup.autosmart.service.ChatbotService;
import com.tecsup.autosmart.service.ConfiguracionTallerService;
import com.tecsup.autosmart.service.FaqService;
import com.tecsup.autosmart.service.ServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotServiceTest {

    private MensajeChatbotRepository mensajeRepository;
    private FaqService faqService;
    private ServicioService servicioService;
    private ConfiguracionTallerService tallerService;
    private ChatbotService chatbot;

    @BeforeEach
    void preparar() {
        mensajeRepository = mock(MensajeChatbotRepository.class);
        faqService = mock(FaqService.class);
        servicioService = mock(ServicioService.class);
        tallerService = mock(ConfiguracionTallerService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T19:00:00Z"), ZoneId.of("America/Lima"));

        Faq yape = new Faq();
        yape.setIdFaq(1);
        yape.setPregunta("¿Aceptan Yape o Plin?");
        yape.setRespuesta("Sí. Puedes pagar con Yape o Plin.");
        yape.setPalabrasClave("yape, plin, pago, pagar");
        Faq garantia = new Faq();
        garantia.setIdFaq(2);
        garantia.setPregunta("¿Los trabajos tienen garantía?");
        garantia.setRespuesta("Sí, 30 días.");
        garantia.setPalabrasClave("garantia, garantía, reclamo");
        when(faqService.activas()).thenReturn(List.of(yape, garantia));

        ServicioResponse aceite = new ServicioResponse();
        aceite.setIdServicio(1L);
        aceite.setNombre("Cambio de aceite");
        aceite.setPrecioBase(new BigDecimal("150"));
        when(servicioService.listarActivos(null, null)).thenReturn(List.of(aceite));

        List<DiaTallerDto> dias = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            DiaTallerDto d = new DiaTallerDto();
            d.setDiaSemana(i);
            d.setAbierto(i != 7);
            d.setHoraApertura("08:00");
            d.setHoraCierre("18:00");
            dias.add(d);
        }
        when(tallerService.listarHorario()).thenReturn(dias);

        List<ManejadorIntencion> manejadores = List.of(
                new SaludoManejador(),
                new HorarioManejador(tallerService),
                new ServiciosManejador(servicioService),
                new FaqManejador(faqService),
                new ManejadorProtegido());
        chatbot = new ChatbotService(manejadores, mensajeRepository, usuarioRepository, reloj);
    }

    @Test
    void saludaCreaLaConversacionYGuardaAmbosMensajes() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("Hola!!", null));

        assertEquals("saludo", res.intencion());
        assertEquals(36, res.idConversacion().length());
        assertTrue(res.opciones().contains(OpcionesChat.VER_SERVICIOS));
        assertFalse(res.requiereLogin());
        verify(mensajeRepository, times(2)).save(any(MensajeChatbot.class));
    }

    @Test
    void respondeLaFaqPorPalabraClaveSinImportarTildesNiMayusculas() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("hola, ¿tienen GARANTÍA?", null));

        assertEquals("faq", res.intencion());
        assertEquals("Sí, 30 días.", res.respuesta());
        assertEquals(Map.of("idFaq", 2), res.datos());
    }

    @Test
    void listaLasPreguntasFrecuentesComoOpciones() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("Preguntas frecuentes", null));

        assertEquals("faq", res.intencion());
        assertEquals(List.of("¿Aceptan Yape o Plin?", "¿Los trabajos tienen garantía?"), res.opciones());
    }

    @Test
    void listaLosServiciosConSuPrecio() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("¿Qué servicios tienen?", null));

        assertEquals("servicios", res.intencion());
        assertTrue(res.respuesta().contains("Cambio de aceite (desde S/ 150.00)"));
        assertEquals(1, ((List<?>) res.datos().get("servicios")).size());
    }

    @Test
    void respondeElHorarioConLosDiasAbiertos() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("¿a qué hora abren?", null));

        assertEquals("horario", res.intencion());
        assertTrue(res.respuesta().contains("Domingo: cerrado"));
        assertEquals(6, ((List<?>) res.datos().get("dias")).size());
    }

    @Test
    void siNoEntiendeSugiereOpciones() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("asdfgh", null));

        assertEquals("no_entendido", res.intencion());
        assertEquals(3, res.opciones().size());
    }

    @Test
    void mantieneLaConversacionValidaYReemplazaLaInvalida() {
        String id = "c7f1e2a0-5b1d-4c2e-9f00-1a2b3c4d5e6f";

        assertEquals(id, chatbot.procesar(null, new ChatbotRequest("hola", id)).idConversacion());
        assertNotEquals("no-es-uuid", chatbot.procesar(null, new ChatbotRequest("hola", "no-es-uuid")).idConversacion());
    }

    @Test
    void pideIniciarSesionSiLaIntencionLoRequiere() {
        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("estado de mi auto", null));

        assertEquals("protegida", res.intencion());
        assertTrue(res.requiereLogin());
    }

    @Test
    void siUnaConsultaFallaRespondeSinRomperElChat() {
        when(servicioService.listarActivos(null, null)).thenThrow(new RuntimeException("BD caida"));

        ChatbotResponse res = chatbot.procesar(null, new ChatbotRequest("ver servicios", null));

        assertEquals("servicios", res.intencion());
        assertTrue(res.respuesta().startsWith("Tuve un problema"));
    }

    private static class ManejadorProtegido implements ManejadorIntencion {
        public String intencion() { return "protegida"; }
        public int prioridad() { return 5; }
        public boolean aplica(MensajeChat m) { return TextoChat.contieneTermino(m, "estado"); }
        public RespuestaChat responder(MensajeChat m) { return RespuestaChat.de(intencion(), "ok", List.of(), null); }
        public boolean requiereSesion() { return true; }
    }
}