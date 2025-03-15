package com.example.catalogoautos.model

import java.util.*

data class Auto(
    val id: String = UUID.randomUUID().toString(),
    var marca: String = "",
    var modelo: String = "",
    var año: Int = 0,
    var color: String = "",
    var precio: Double = 0.0,
    var estado: String = "Nuevo", // "Nuevo" o "Usado"
    var kilometraje: Int = 0,
    var fotoPath: String = "", // Añadimos este campo para la ruta de la foto
    var detallesTecnicos: String = "",
    val fechaRegistro: Date = Date()
)