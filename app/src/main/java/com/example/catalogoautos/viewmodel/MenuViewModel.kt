package com.example.catalogoautos.viewmodel

import android.app.Application
import android.util.Log
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

    private val TAG = "MenuViewModel"


    private val _usuarioActual = MutableStateFlow<String?>(null)
    val usuarioActual: StateFlow<String?> = _usuarioActual


    private val _totalAutos = MutableStateFlow(0)
    val totalAutos: StateFlow<Int> = _totalAutos


    private var lastSelectedAutoId: Int? = null

    init {
        Log.d(TAG, "MenuViewModel inicializado")

        cargarTotalAutos()

        cargarUsuarioActual()
    }

    private fun cargarTotalAutos() {
        viewModelScope.launch {
            try {

                val total = autoRepository.obtenerTodosLosAutos().size
                Log.d(TAG, "Total de autos cargados: $total")
                _totalAutos.value = total
            } catch (e: Exception) {

                Log.e(TAG, "Error al cargar total de autos: ${e.message}")
                _totalAutos.value = 0
            }
        }
    }


    private fun cargarUsuarioActual() {
        viewModelScope.launch {
            try {
                val usuario = usuarioRepository.getUsuarioActual()
                Log.d(TAG, "Usuario cargado: ${usuario?.nombre} ${usuario?.apellido}")
                if (usuario != null) {

                    _usuarioActual.value = "${usuario.nombre} ${usuario.apellido}"
                } else {
                    Log.e(TAG, "No se pudo cargar el usuario")
                    _usuarioActual.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar usuario: ${e.message}")
                _usuarioActual.value = null
            }
        }
    }

    fun logout() {
        Log.d(TAG, "Cerrando sesión de usuario")
        usuarioRepository.logout()
        _usuarioActual.value = null // Limpiar el estado del usuario actual
    }


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