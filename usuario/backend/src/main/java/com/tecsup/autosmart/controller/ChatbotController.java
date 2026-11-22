package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ChatbotRequest;
import com.tecsup.autosmart.dto.ChatbotResponse;
import com.tecsup.autosmart.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chatbot/messages")
    public ResponseEntity<ChatbotResponse> enviar(@Valid @RequestBody ChatbotRequest request, Authentication auth) {
        String correo = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                ? auth.getName()
                : null;
        return ResponseEntity.ok(chatbotService.procesar(correo, request));
    }
}