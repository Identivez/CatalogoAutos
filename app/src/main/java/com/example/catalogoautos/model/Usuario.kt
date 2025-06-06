package com.example.catalogoautos.model

import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class Usuario(
    val usuarioId: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val rol: String,
    val fechaRegistro: String
) {

    companion object {
        fun fromJson(json: String?): Usuario {
            val jsonObject = JSONObject(json ?: "{}")
            return Usuario(
                usuarioId = jsonObject.optInt("usuario_id", 0),
                nombre = jsonObject.optString("nombre", ""),
                apellido = jsonObject.optString("apellido", ""),
                email = jsonObject.optString("email", ""),
                password = jsonObject.optString("password", ""),
                rol = jsonObject.optString("rol", ""),
                fechaRegistro = jsonObject.optString("fecha_registro", "")
            )
        }


        fun toJson(usuarioId: Int, nombre: String, apellido: String, email: String, password: String, rol: String, fechaRegistro: String): String {
            val jsonObject = JSONObject()
            jsonObject.put("usuario_id", usuarioId)
            jsonObject.put("nombre", nombre)
            jsonObject.put("apellido", apellido)
            jsonObject.put("email", email)
            jsonObject.put("password", password)
            jsonObject.put("rol", rol)
            jsonObject.put("fecha_registro", fechaRegistro)
            return jsonObject.toString()
        }
    }


    fun getFechaRegistroAsLocalDateTime(): LocalDateTime? {
        return if (fechaRegistro.isNotEmpty()) {
            try {
                LocalDateTime.parse(fechaRegistro, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {

                null
            }
        } else {
            null
        }
    }
}