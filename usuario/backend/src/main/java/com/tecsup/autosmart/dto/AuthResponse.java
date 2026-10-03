package com.tecsup.autosmart.dto;

public class AuthResponse {
    private String token;
    private String tipo = "Bearer";
    private String correo;
    private String rol;

    public AuthResponse(String token, String correo, String rol) {
        this.token = token;
        this.correo = correo;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public String getCorreo() { return correo; }
    public String getRol() { return rol; }
}