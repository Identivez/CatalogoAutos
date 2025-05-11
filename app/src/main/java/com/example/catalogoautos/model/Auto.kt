package com.example.catalogoautos.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Auto(
    @SerializedName("auto_id") val auto_id: Int = 0,
    @SerializedName("n_serie") var n_serie: String = "",
    @SerializedName("sku") var sku: String = "",
    @SerializedName("marca_id") var marca_id: Int = 0,
    @SerializedName("modelo") var modelo: String = "",
    @SerializedName("anio") var anio: Int = 0,
    @SerializedName("color") var color: String = "",
    @SerializedName("precio") var precio: Double = 0.0,
    @SerializedName("stock") var stock: Int = 0,
    @SerializedName("descripcion") var descripcion: String = "",
    @SerializedName("disponibilidad") var disponibilidad: Boolean = true,
    @SerializedName("fecha_registro") val fecha_registro: LocalDateTime = LocalDateTime.now(),
    @SerializedName("fecha_actualizacion") var fecha_actualizacion: LocalDateTime = LocalDateTime.now()
)