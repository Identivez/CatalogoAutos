package com.example.catalogoautos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.repository.UsuarioRepository
import kotlinx.coroutines.flow.StateFlow

class MenuViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {

    // Exponemos directamente el StateFlow para que la actividad pueda observarlo
    val usuarioActual: StateFlow<String?> = usuarioRepository.usuarioActual

    fun logout() {
        usuarioRepository.logout()
    }

    // Mantenemos este método para compatibilidad con el código existente
    fun getUsuarioActual(): String? {
        return usuarioRepository.usuarioActual.value
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
                return MenuViewModel(UsuarioRepository.getInstance()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}