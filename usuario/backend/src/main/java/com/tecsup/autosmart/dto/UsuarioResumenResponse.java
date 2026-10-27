package com.tecsup.autosmart.dto;

public class UsuarioResumenResponse {

    private Integer idUsuario;
    private String nombre;
    private String correo;
    private String rol;
    private Boolean activo;
    private String creadoEn;
    private String estadoCambiadoEn;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    public String getEstadoCambiadoEn() { return estadoCambiadoEn; }
    public void setEstadoCambiadoEn(String estadoCambiadoEn) { this.estadoCambiadoEn = estadoCambiadoEn; }
}