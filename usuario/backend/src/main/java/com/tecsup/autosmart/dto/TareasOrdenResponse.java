package com.tecsup.autosmart.dto;

import java.util.List;

public class TareasOrdenResponse {

    private Long idOrden;
    private int totalTareas;
    private int completadas;
    private int porcentajeAvance;
    private List<Tarea> tareas;

    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public int getTotalTareas() { return totalTareas; }
    public void setTotalTareas(int totalTareas) { this.totalTareas = totalTareas; }
    public int getCompletadas() { return completadas; }
    public void setCompletadas(int completadas) { this.completadas = completadas; }
    public int getPorcentajeAvance() { return porcentajeAvance; }
    public void setPorcentajeAvance(int porcentajeAvance) { this.porcentajeAvance = porcentajeAvance; }
    public List<Tarea> getTareas() { return tareas; }
    public void setTareas(List<Tarea> tareas) { this.tareas = tareas; }

    public static class Tarea {
        private Long idTarea;
        private String descripcion;
        private boolean completada;
        private String fechaCreacion;
        private String fechaCompletada;

        public Tarea(Long idTarea, String descripcion, boolean completada, String fechaCreacion, String fechaCompletada) {
            this.idTarea = idTarea;
            this.descripcion = descripcion;
            this.completada = completada;
            this.fechaCreacion = fechaCreacion;
            this.fechaCompletada = fechaCompletada;
        }

        public Long getIdTarea() { return idTarea; }
        public String getDescripcion() { return descripcion; }
        public boolean isCompletada() { return completada; }
        public String getFechaCreacion() { return fechaCreacion; }
        public String getFechaCompletada() { return fechaCompletada; }
    }
}