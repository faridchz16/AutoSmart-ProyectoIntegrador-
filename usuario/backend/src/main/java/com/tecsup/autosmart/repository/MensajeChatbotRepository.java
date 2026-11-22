package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.MensajeChatbot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeChatbotRepository extends JpaRepository<MensajeChatbot, Long> {

    List<MensajeChatbot> findTop10ByIdConversacionOrderByIdMensajeDesc(String idConversacion);
}