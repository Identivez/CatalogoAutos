package com.example.catalogoautos.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DetalleAutoViewModel(private val repository: AutoRepository) : ViewModel() {

    // Estados para el auto seleccionado
    private val _auto = MutableLiveData<Auto?>(null)
    val auto: LiveData<Auto?> = _auto

    // Estados para mostrar información formateada
    private val _precio = MutableLiveData<String>("")
    val precio: LiveData<String> = _precio

    private val _fechaRegistro = MutableLiveData<String>("")
    val fechaRegistro: LiveData<String> = _fechaRegistro

    private val _fechaActualizacion = MutableLiveData<String>("")
    val fechaActualizacion: LiveData<String> = _fechaActualizacion

    // Estado para indicar carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado para mostrar errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Formateador de fechas
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /**
     * Carga un auto por su ID desde el repositorio y actualiza los LiveData
     */
    fun cargarAuto(autoId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val autoEncontrado = repository.obtenerAutoPorId(autoId)
                _auto.value = autoEncontrado

                // Formatear datos para presentación
                autoEncontrado?.let { auto ->
                    _precio.value = formatearPrecio(auto.precio)
                    _fechaRegistro.value = formatearFecha(auto.fecha_registro)
                    _fechaActualizacion.value = formatearFecha(auto.fecha_actualizacion)
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al cargar el auto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Formatea el precio para visualización
     */
    private fun formatearPrecio(precio: Double): String {
        return "$${String.format("%,.2f", precio)}"
    }

    /**
     * Formatea la fecha para visualización
     */
    private fun formatearFecha(fecha: LocalDateTime): String {
        return fecha.format(dateFormatter)
    }

    /**
     * Actualiza la disponibilidad de un auto
     */
    fun actualizarDisponibilidad(autoId: Int, disponible: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val autoActual = repository.obtenerAutoPorId(autoId)
                autoActual?.let { auto ->
                    val autoActualizado = auto.copy(
                        disponibilidad = disponible,
                        fecha_actualizacion = LocalDateTime.now()
                    )
                    repository.actualizarAuto(autoActualizado)
                    cargarAuto(autoId) // Recarga el auto para reflejar cambios
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar disponibilidad: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina un auto
     * @return true si se elimina correctamente, false en caso contrario
     */
    fun eliminarAuto(autoId: Int): Boolean {
        var eliminado = false
        try {
            repository.eliminarAuto(autoId)
            eliminado = true
        } catch (e: Exception) {
            _error.value = "Error al eliminar el auto: ${e.message}"
        }
        return eliminado
    }

    /**
     * Verifica si hay stock disponible
     */
    fun hayStockDisponible(): Boolean {
        return _auto.value?.stock ?: 0 > 0
    }

    /**
     * Proporciona información sobre marcado de nuevo campo por su novedad
     */
    fun debeMostrarIndicadorNuevoCampo(campo: String): Boolean {
        // Por ejemplo, podríamos marcar los nuevos campos durante 30 días
        return when(campo) {
            "n_serie", "sku" -> true
            else -> false
        }
    }

    /**
     * Factory para crear el ViewModel con el repositorio
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetalleAutoViewModel::class.java)) {
                return DetalleAutoViewModel(AutoRepository.getInstance(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}