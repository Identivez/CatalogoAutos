package com.example.catalogoautos.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.catalogoautos.model.Usuario

class UsuarioRepository(private val context: Context) {

    private val TAG = "UsuarioRepository"

    // Acceder a SharedPreferences
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("usuario_preferences", Context.MODE_PRIVATE)

    // Guardar el usuario actual
    fun setUsuarioActual(usuarioId: Int, nombre: String, apellido: String, email: String, rol: String, fechaRegistro: String) {
        try {
            Log.d(TAG, "Guardando usuario: ID=$usuarioId, Nombre=$nombre, Apellido=$apellido")

            val editor = sharedPreferences.edit()
            editor.putInt("usuario_id", usuarioId)  // Usar putInt para IDs
            editor.putString("usuario_nombre", nombre)
            editor.putString("usuario_apellido", apellido)
            editor.putString("usuario_email", email)
            editor.putString("usuario_rol", rol)
            editor.putString("usuario_fecha_registro", fechaRegistro)

            // Usar apply() para guardado asíncrono, pero agregar un log después para verificar
            editor.apply()

            // Verificar inmediatamente si se guardó
            val verificarNombre = sharedPreferences.getString("usuario_nombre", "")
            val verificarId = sharedPreferences.getInt("usuario_id", 0)
            Log.d(TAG, "Verificación de guardado: ID=$verificarId, Nombre=$verificarNombre")

            Log.d(TAG, "Usuario guardado con éxito: $nombre $apellido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar el usuario actual", e)
        }
    }


    fun getUsuarioActual(): Usuario? {
        try {
            val usuarioId = sharedPreferences.getInt("usuario_id", 0)
            val nombre = sharedPreferences.getString("usuario_nombre", "") ?: ""
            val apellido = sharedPreferences.getString("usuario_apellido", "") ?: ""
            val email = sharedPreferences.getString("usuario_email", "") ?: ""
            val rol = sharedPreferences.getString("usuario_rol", "") ?: ""
            val fechaRegistro = sharedPreferences.getString("usuario_fecha_registro", "") ?: ""

            Log.d(TAG, "Intentando recuperar usuario: ID=$usuarioId, Nombre=$nombre, Apellido=$apellido")

            if (usuarioId > 0 && nombre.isNotEmpty() && email.isNotEmpty()) {
                val usuario = Usuario(
                    usuarioId = usuarioId,
                    nombre = nombre,
                    apellido = apellido,
                    email = email,
                    password = "",
                    rol = rol,
                    fechaRegistro = fechaRegistro
                )
                Log.d(TAG, "Usuario recuperado con éxito: $nombre $apellido")
                return usuario
            } else {
                Log.e(TAG, "No se encontró un usuario válido en SharedPreferences. ID=$usuarioId, Nombre=$nombre")
                // Imprimir todas las keys en SharedPreferences para depuración
                val allPrefs = sharedPreferences.all
                Log.d(TAG, "Contenido de SharedPreferences: $allPrefs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener el usuario actual", e)
        }
        return null
    }

    // Eliminar el usuario actual (logout)
    fun logout() {
        try {
            val editor = sharedPreferences.edit()
            editor.clear()  // Limpiar todas las preferencias relacionadas con el usuario
            editor.commit()  // Usar commit() en lugar de apply() para asegurar que se complete antes de continuar
            Log.d(TAG, "Usuario deslogueado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al hacer logout", e)
        }
    }
}