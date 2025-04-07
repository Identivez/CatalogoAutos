package com.example.catalogoautos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CatalogoAutosViewModel(private val repository: AutoRepository) : ViewModel() {

    // Filtros
    private val _searchQuery = MutableStateFlow("")
    private val _disponibilidadFiltro = MutableStateFlow<Boolean?>(null) // null = todos
    private val _marcaFiltro = MutableStateFlow<Int?>(null) // null = todas las marcas
    private val _precioMaximo = MutableStateFlow<Double?>(null)

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Lista de autos filtrada
    val autos = combine(
        repository.autos,
        _searchQuery,
        _disponibilidadFiltro,
        _marcaFiltro,
        _precioMaximo
    ) { autos, query, disponibilidad, marcaId, precioMax ->
        _isLoading.value = true

        try {
            val filteredAutos = autos.filter { auto ->
                // Filtrar por término de búsqueda (en modelo)
                val matchesQuery = query.isEmpty() ||
                        auto.modelo.contains(query, ignoreCase = true)

                // Filtrar por disponibilidad
                val matchesDisponibilidad = disponibilidad == null ||
                        auto.disponibilidad == disponibilidad

                // Filtrar por marca
                val matchesMarca = marcaId == null ||
                        auto.marca_id == marcaId

                // Filtrar por precio máximo
                val matchesPrecio = precioMax == null ||
                        auto.precio <= precioMax

                matchesQuery && matchesDisponibilidad && matchesMarca && matchesPrecio
            }

            filteredAutos
        } catch (e: Exception) {
            _error.value = "Error al filtrar autos: ${e.message}"
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    // Métodos para actualizar filtros
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDisponibilidadFiltro(disponibilidad: Boolean?) {
        _disponibilidadFiltro.value = disponibilidad
    }

    fun setMarcaFiltro(marcaId: Int?) {
        _marcaFiltro.value = marcaId
    }

    fun setPrecioMaximo(precio: Double?) {
        _precioMaximo.value = precio
    }

    fun limpiarFiltros() {
        _searchQuery.value = ""
        _disponibilidadFiltro.value = null
        _marcaFiltro.value = null
        _precioMaximo.value = null
    }

    // Factory para crear el ViewModel con el repositorio
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogoAutosViewModel::class.java)) {
                return CatalogoAutosViewModel(AutoRepository.getInstance(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}