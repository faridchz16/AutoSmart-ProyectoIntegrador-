package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    private String correo;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}