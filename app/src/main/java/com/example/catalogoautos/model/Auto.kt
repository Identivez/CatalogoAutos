package com.example.catalogoautos.model

import java.time.LocalDateTime
import java.util.*

data class Auto(
    val auto_id: Int = 0,
    var n_serie: String = "",
    var sku: String = "",
    var marca_id: Int = 0,
    var modelo: String = "",
    var anio: Int = 0,
    var color: String = "",
    var precio: Double = 0.0,
    var stock: Int = 0,
    var descripcion: String = "",
    var disponibilidad: Boolean = true,
    val fecha_registro: LocalDateTime = LocalDateTime.now(),
    var fecha_actualizacion: LocalDateTime = LocalDateTime.now()
)