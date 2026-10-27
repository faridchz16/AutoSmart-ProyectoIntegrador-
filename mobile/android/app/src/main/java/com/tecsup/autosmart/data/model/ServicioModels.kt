package com.tecsup.autosmart.data.model

import com.google.gson.annotations.SerializedName

data class Servicio(
    @SerializedName(value = "idServicio", alternate = ["id", "id_servicio"])
    val idServicio: Long = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    @SerializedName(value = "precioBase", alternate = ["precio", "precio_base"])
    val precioBase: Double = 0.0,
    @SerializedName(value = "duracionMinutos", alternate = ["duracion", "duracion_minutos"])
    val duracionMinutos: Int = 0
)