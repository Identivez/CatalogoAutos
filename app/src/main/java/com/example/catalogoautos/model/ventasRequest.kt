package com.example.catalogoautos.model

import java.math.BigDecimal

data class VentaRequest(
    val nSerie: String,
    val cantidad: Int,
    val precio: BigDecimal,  // Cambiado de String a BigDecimal
    val estatus: String = "COMPLETADA"
)