package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResetPasswordRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    private String correo;

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe tener 6 digitos")
    private String codigo;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "La contrasena debe tener minimo 8 caracteres, al menos una mayuscula y un numero")
    private String nuevaPassword;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNuevaPassword() { return nuevaPassword; }
    public void setNuevaPassword(String nuevaPassword) { this.nuevaPassword = nuevaPassword; }
}