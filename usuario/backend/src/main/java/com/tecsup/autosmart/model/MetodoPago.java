package com.tecsup.autosmart.model;

import jakarta.persistence.*;

@Entity
@Table(name = "metodos_pago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo")
    private Integer idMetodo;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "en_linea", nullable = false)
    private Boolean enLinea = true;

    @Column(nullable = false)
    private Boolean habilitado = true;

    @Column(nullable = false)
    private Integer orden = 0;

    public Integer getIdMetodo() { return idMetodo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getEnLinea() { return enLinea; }
    public void setEnLinea(Boolean enLinea) { this.enLinea = enLinea; }
    public Boolean getHabilitado() { return habilitado; }
    public void setHabilitado(Boolean habilitado) { this.habilitado = habilitado; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}