package com.example.catalogoautos.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para el Catálogo de Autos.
 *
 * Este ViewModel maneja la lógica para mostrar y filtrar el catálogo de autos disponibles.
 * A diferencia del InventarioViewModel, este está orientado a usuarios finales y no
 * incluye funcionalidades de edición.
 */
class CatalogoAutosViewModel(private val autoRepository: AutoRepository) : ViewModel() {

    // Estado de carga
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Término de búsqueda
    private val _searchQuery = MutableStateFlow("")

    // Filtro de estado (Nuevo, Usado, Todos)
    private val _estadoFiltro = MutableStateFlow("Todos")

    // Filtro de precio máximo
    private val _precioMaximo = MutableStateFlow<Double?>(null)

    // Autos filtrados según los criterios aplicados
    val autos = autoRepository.autos.map { listaAutos ->
        val query = _searchQuery.value
        val estado = _estadoFiltro.value
        val precioMax = _precioMaximo.value

        _isLoading.value = false

        var resultado = listaAutos

        // Filtrar por búsqueda
        if (query.isNotBlank()) {
            resultado = resultado.filter {
                it.marca.contains(query, ignoreCase = true) ||
                        it.modelo.contains(query, ignoreCase = true) ||
                        it.año.toString().contains(query)
            }
        }

        // Filtrar por estado
        if (estado != "Todos") {
            resultado = resultado.filter { it.estado == estado }
        }

        // Filtrar por precio máximo
        if (precioMax != null) {
            resultado = resultado.filter { it.precio <= precioMax }
        }

        // Ordenar por fecha de registro (más reciente primero)
        resultado = resultado.sortedByDescending { it.fechaRegistro }

        Log.d(TAG, "Resultado filtrado: ${resultado.size} autos")
        resultado
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        Log.d(TAG, "ViewModel inicializado")
        try {
            // Iniciar la carga de datos
            cargarAutos()
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ViewModel: ${e.message}", e)
            _error.value = "Error al cargar los datos: ${e.message}"
        }
    }

    // Método para actualizar el término de búsqueda
    fun setSearchQuery(query: String) {
        Log.d(TAG, "Actualizando query de búsqueda a: '$query'")
        _searchQuery.value = query
    }

    // Método para filtrar por estado del auto
    fun setEstadoFiltro(estado: String) {
        Log.d(TAG, "Actualizando filtro de estado a: '$estado'")
        _estadoFiltro.value = estado
    }

    // Método para establecer un precio máximo como filtro
    fun setPrecioMaximo(precio: Double?) {
        Log.d(TAG, "Actualizando precio máximo a: $precio")
        _precioMaximo.value = precio
    }

    // Método para limpiar todos los filtros
    fun limpiarFiltros() {
        Log.d(TAG, "Limpiando todos los filtros")
        _searchQuery.value = ""
        _estadoFiltro.value = "Todos"
        _precioMaximo.value = null
    }

    // Método para obtener un auto específico por su ID
    fun obtenerAutoPorId(id: String): Auto? {
        val auto = autoRepository.obtenerAutoPorId(id)
        Log.d(TAG, "Buscando auto ID: $id - Encontrado: ${auto != null}")
        return auto
    }

    // Método para forzar la carga de autos desde el repositorio
    fun cargarAutos() {
        Log.d(TAG, "Solicitando carga de autos al repositorio")
        _isLoading.value = true
        _error.value = null

        try {
            // No es necesario hacer nada especial aquí ya que el repositorio maneja la actualización
            // a través del StateFlow y la UI se actualizará automáticamente

            // Simulamos un breve retraso para mostrar el estado de carga
            viewModelScope.launch {
                delay(500)
                _isLoading.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar autos: ${e.message}", e)
            _error.value = "Error al cargar los datos: ${e.message}"
            _isLoading.value = false
        }
    }

    // Métodos adicionales para filtros avanzados (para futura implementación)
    fun setAnioFiltro(anio: Int?) {
        // En esta versión simplificada, este método no hace nada
        // pero se mantiene para compatibilidad con la actividad
    }

    fun setPrecioMinimo(precio: Double?) {
        // En esta versión simplificada, este método no hace nada
        // pero se mantiene para compatibilidad con la actividad
    }

    fun setKilometrajeMaximo(km: Int?) {
        // En esta versión simplificada, este método no hace nada
        // pero se mantiene para compatibilidad con la actividad
    }

    fun setTipoCombustible(tipo: String) {
        // En esta versión simplificada, este método no hace nada
        // pero se mantiene para compatibilidad con la actividad
    }

    // Factory para crear instancias del ViewModel con el repositorio adecuado
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogoAutosViewModel::class.java)) {
                Log.d("$TAG.Factory", "Creando ViewModel con contexto: $context")
                return CatalogoAutosViewModel(AutoRepository.getInstance(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val TAG = "CatalogoAutosViewModel"
    }
}