package com.tecsup.autosmart.dto;

public class AuthResponse {
    private String token;
    private String tipo = "Bearer";
    private Integer idUsuario;
    private String nombre;
    private String correo;
    private String rol;

    public AuthResponse(String token, Integer idUsuario, String nombre, String correo, String rol) {
        this.token = token;
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public Integer getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getRol() { return rol; }
}