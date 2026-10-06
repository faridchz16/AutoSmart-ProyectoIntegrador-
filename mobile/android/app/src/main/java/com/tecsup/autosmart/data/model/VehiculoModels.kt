package com.tecsup.autosmart.data.model

import com.google.gson.annotations.SerializedName

// Modelo para recibir del backend (GET)
data class VehiculoResponse(
    @SerializedName("idVehiculo") val idVehiculo: Long,
    @SerializedName("placa") val placa: String,
    @SerializedName("marca") val marca: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("anio") val anio: Int,
    @SerializedName("kilometraje") val kilometraje: Int,
    @SerializedName("idCliente") val idCliente: Int?,
    @SerializedName("nombreCliente") val nombreCliente: String?
)

// Modelo para enviar al backend (POST)
data class VehiculoRequest(
    @SerializedName("placa") val placa: String,
    @SerializedName("marca") val marca: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("anio") val anio: Int,
    @SerializedName("kilometraje") val kilometraje: Int,
    @SerializedName("idCliente") val idCliente: Int
)