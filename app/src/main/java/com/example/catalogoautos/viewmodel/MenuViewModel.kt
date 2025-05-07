package com.example.catalogoautos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.repository.AutoRepository
import com.example.catalogoautos.repository.UsuarioRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MenuViewModel(
    application: Application,
    private val usuarioRepository: UsuarioRepository,
    private val autoRepository: AutoRepository
) : AndroidViewModel(application) {

    // StateFlow para el usuario actual
    private val _usuarioActual = MutableStateFlow<String?>(null)
    val usuarioActual: StateFlow<String?> = _usuarioActual

    // StateFlow para el total de autos en inventario
    private val _totalAutos = MutableStateFlow(0)
    val totalAutos: StateFlow<Int> = _totalAutos

    // Variable para almacenar el último auto seleccionado
    private var lastSelectedAutoId: Int? = null

    init {
        // Inicializar el contador de autos
        cargarTotalAutos()
        // Inicializar el usuario actual
        cargarUsuarioActual()
    }

    private fun cargarTotalAutos() {
        viewModelScope.launch {
            try {
                // Obtenemos el total de autos en el inventario
                val total = autoRepository.obtenerTodosLosAutos().size
                _totalAutos.value = total
            } catch (e: Exception) {
                // Si no se puede obtener el total real, usar un valor predeterminado
                _totalAutos.value = 0
            }
        }
    }

    // Método para cargar el usuario actual desde el repositorio
    private fun cargarUsuarioActual() {
        viewModelScope.launch {
            val usuario = usuarioRepository.getUsuarioActual()
            _usuarioActual.value = usuario?.nombre
        }
    }

    fun logout() {
        usuarioRepository.logout()
        _usuarioActual.value = null // Limpiar el estado del usuario actual
    }

    // Métodos para manejar el último auto seleccionado (para la función de detalles)
    fun setLastSelectedAutoId(autoId: Int) {
        lastSelectedAutoId = autoId
    }

    fun getLastSelectedAutoId(): Int? {
        return lastSelectedAutoId
    }

    class Factory(
        private val application: Application,
        private val usuarioRepository: UsuarioRepository,
        private val autoRepository: AutoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
                return MenuViewModel(application, usuarioRepository, autoRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}