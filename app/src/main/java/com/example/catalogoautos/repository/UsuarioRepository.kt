
package com.example.catalogoautos.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsuarioRepository {
    private val _usuarioActual = MutableStateFlow<String?>(null)
    val usuarioActual = _usuarioActual.asStateFlow()

    // En un sistema real, esto verificaría contra una base de datos
    fun login(username: String, password: String): Boolean {
        return if (username == "admin" && password == "admin123") {
            _usuarioActual.value = username
            true
        } else {
            false
        }
    }

    fun logout() {
        _usuarioActual.value = null
    }

    fun estaLogueado(): Boolean {
        return _usuarioActual.value != null
    }

    companion object {
        @Volatile
        private var INSTANCE: UsuarioRepository? = null

        fun getInstance(): UsuarioRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UsuarioRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}