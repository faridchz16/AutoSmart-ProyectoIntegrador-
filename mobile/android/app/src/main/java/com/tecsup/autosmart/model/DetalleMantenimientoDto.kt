package com.tecsup.autosmart.model

import java.math.BigDecimal

data class DetalleMantenimientoDto(
    val idOrden: Long,
    val idCita: Long,
    val ticket: String?,
    val estado: String?,
    val servicio: String?,
    val precioServicio: BigDecimal?,
    val placa: String?,
    val vehiculo: String?,
    val mecanico: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val diagnostico: String?,
    val checklist: List<ChecklistItemDto?>?,
    val deficiencias: List<DeficienciaResponse?>?,
    val tareas: TareasMantenimientoDto?,
    val presupuesto: ResumenPresupuestoDto?,
    val repuestosConsumidos: List<RepuestoConsumidoDto?>?,
    val costoRepuestos: BigDecimal?,
    val totalEstimado: BigDecimal?
)

data class TareasMantenimientoDto(
    val porcentajeAvance: Int?,
    val totalTareas: Int?,
    val completadas: Int?,
    val tareas: List<TareaMantenimientoDto?>?
)

data class TareaMantenimientoDto(
    val descripcion: String?,
    val completada: Boolean?,
    val fechaCompletada: String?
)

data class ResumenPresupuestoDto(
    val idPresupuesto: Long,
    val estado: String?,
    val descripcion: String?,
    val total: BigDecimal?,
    val items: List<PresupuestoItemDto?>?
)

data class RepuestoConsumidoDto(
    val codigoMaterial: String?,
    val cantidad: Int?,
    val costoUnitario: BigDecimal?,
    val fecha: String?
)
