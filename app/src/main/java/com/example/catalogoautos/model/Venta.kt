// Venta.kt
package com.example.catalogoautos.model

import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Venta(
    val ventaId: Int = 0,
    val nSerie: String,
    val cantidad: Int = 1,
    val precio: BigDecimal,
    val estatus: String = "COMPLETADA",
    val fechaVenta: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun fromJson(json: String?): Venta {
            val jsonObject = JSONObject(json ?: "{}")

            // Parsear fecha
            val fechaStr = jsonObject.optString("fechaVenta", "")
            val fecha = if (fechaStr.isNotEmpty()) {
                try {
                    LocalDateTime.parse(fechaStr)
                } catch (e: Exception) {
                    LocalDateTime.now()
                }
            } else {
                LocalDateTime.now()
            }

            return Venta(
                ventaId = jsonObject.optInt("ventaId", 0),
                nSerie = jsonObject.optString("nSerie", ""),
                cantidad = jsonObject.optInt("cantidad", 1),
                precio = BigDecimal(jsonObject.optString("precio", "0")),
                estatus = jsonObject.optString("estatus", "COMPLETADA"),
                fechaVenta = fecha
            )
        }

        fun toJson(venta: Venta): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("ventaId", venta.ventaId)
            jsonObject.put("nSerie", venta.nSerie)
            jsonObject.put("cantidad", venta.cantidad)
            jsonObject.put("precio", venta.precio.toString())
            jsonObject.put("estatus", venta.estatus)
            jsonObject.put("fechaVenta", venta.fechaVenta.toString())
            return jsonObject
        }
    }
}