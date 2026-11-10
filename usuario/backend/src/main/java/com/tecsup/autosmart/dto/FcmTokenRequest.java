package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FcmTokenRequest {

    @NotBlank(message = "El token del dispositivo es obligatorio")
    @Size(max = 255, message = "El token no puede superar 255 caracteres")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}