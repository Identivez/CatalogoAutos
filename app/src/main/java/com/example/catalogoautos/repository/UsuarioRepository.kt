package com.example.catalogoautos.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.catalogoautos.model.Usuario

class UsuarioRepository(private val context: Context) {

    // Acceder a SharedPreferences
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("usuario_preferences", Context.MODE_PRIVATE)

    // Guardar el usuario actual
    fun setUsuarioActual(usuarioId: Int, nombre: String, apellido: String, email: String, rol: String, fechaRegistro: String) {
        try {
            val editor = sharedPreferences.edit()
            editor.putString("usuario_id", usuarioId.toString())
            editor.putString("usuario_nombre", nombre)
            editor.putString("usuario_apellido", apellido)
            editor.putString("usuario_email", email)
            editor.putString("usuario_rol", rol)
            editor.putString("usuario_fecha_registro", fechaRegistro)
            editor.apply() // Guardar de manera asíncrona
            Log.d("UsuarioRepository", "Usuario guardado con éxito: $nombre")
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Error al guardar el usuario actual", e)
        }
    }

    // Obtener el usuario actual
    fun getUsuarioActual(): Usuario? {
        try {
            val usuarioIdStr = sharedPreferences.getString("usuario_id", null)
            val usuarioId = usuarioIdStr?.toIntOrNull() ?: 0
            val nombre = sharedPreferences.getString("usuario_nombre", null) ?: ""
            val apellido = sharedPreferences.getString("usuario_apellido", null) ?: ""
            val email = sharedPreferences.getString("usuario_email", null) ?: ""
            val rol = sharedPreferences.getString("usuario_rol", null) ?: ""
            val fechaRegistro = sharedPreferences.getString("usuario_fecha_registro", null) ?: ""

            if (usuarioId > 0 && nombre.isNotEmpty() && apellido.isNotEmpty() && email.isNotEmpty() && rol.isNotEmpty()) {
                return Usuario(
                    usuarioId = usuarioId,
                    nombre = nombre,
                    apellido = apellido,
                    email = email,
                    password = "", // Se puede poner un valor vacío si no lo tienes
                    rol = rol,
                    fechaRegistro = fechaRegistro // La fecha ya es un String
                )
            }
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Error al obtener el usuario actual", e)
        }
        return null
    }

    // Eliminar el usuario actual (logout)
    fun logout() {
        try {
            val editor = sharedPreferences.edit()
            editor.remove("usuario_id")
            editor.remove("usuario_nombre")
            editor.remove("usuario_apellido")
            editor.remove("usuario_email")
            editor.remove("usuario_rol")
            editor.remove("usuario_fecha_registro")
            editor.apply()
            Log.d("UsuarioRepository", "Usuario deslogueado exitosamente")
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Error al hacer logout", e)
        }
    }
}