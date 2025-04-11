package com.example.catalogoautos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CatalogoAutosViewModel(private val repository: AutoRepository) : ViewModel() {

    // Enumeración para definir por qué campo se está filtrando la búsqueda de texto
    enum class TipoBusqueda {
        MODELO, NUMERO_SERIE, SKU, COLOR, TODOS
    }

    // Filtros básicos
    private val _searchQuery = MutableStateFlow("")
    private val _tipoBusqueda = MutableStateFlow(TipoBusqueda.MODELO) // Por defecto, buscar por modelo
    private val _disponibilidadFiltro = MutableStateFlow<Boolean?>(null) // null = todos
    private val _precioMinimo = MutableStateFlow<Double?>(null)
    private val _precioMaximo = MutableStateFlow<Double?>(null)
    private val _anioMinimo = MutableStateFlow<Int?>(null)
    private val _anioMaximo = MutableStateFlow<Int?>(null)

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Estado de filtros activos - útil para la UI
    private val _hayFiltrosActivos = MutableStateFlow(false)
    val hayFiltrosActivos: StateFlow<Boolean> = _hayFiltrosActivos

    // Lista de autos filtrada
    private val _autos = MutableStateFlow<List<Auto>>(emptyList())
    val autos = _autos.asStateFlow()

    // Inicializar observadores
    init {
        // Observar cambios en repository.autos
        viewModelScope.launch {
            repository.autos.collect { listaAutos ->
                aplicarFiltros()
            }
        }

        // Observar cambios en filtros
        viewModelScope.launch {
            _searchQuery.collect { aplicarFiltros() }
        }
        viewModelScope.launch {
            _tipoBusqueda.collect { aplicarFiltros() }
        }
        viewModelScope.launch {
            _disponibilidadFiltro.collect { aplicarFiltros() }
        }
        viewModelScope.launch {
            _precioMinimo.collect { aplicarFiltros() }
        }
        viewModelScope.launch {
            _precioMaximo.collect { aplicarFiltros() }
        }
        viewModelScope.launch {
            _anioMinimo.collect { aplicarFiltros() }
        }
        viewModelScope.launch {
            _anioMaximo.collect { aplicarFiltros() }
        }
    }

    // Aplicar filtros cuando cambia cualquier criterio
    private fun aplicarFiltros() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val listaAutos = repository.obtenerTodosLosAutos()
                val query = _searchQuery.value
                val tipo = _tipoBusqueda.value
                val disponibilidad = _disponibilidadFiltro.value
                val precioMin = _precioMinimo.value
                val precioMax = _precioMaximo.value
                val anioMin = _anioMinimo.value
                val anioMax = _anioMaximo.value

                val filteredAutos = listaAutos.filter { auto ->
                    // Filtrar por término de búsqueda según el campo seleccionado
                    val matchesQuery = query.isEmpty() || when (tipo) {
                        TipoBusqueda.MODELO -> auto.modelo.contains(query, ignoreCase = true)
                        TipoBusqueda.NUMERO_SERIE -> auto.n_serie.contains(query, ignoreCase = true)
                        TipoBusqueda.SKU -> auto.sku.contains(query, ignoreCase = true)
                        TipoBusqueda.COLOR -> auto.color.contains(query, ignoreCase = true)
                        TipoBusqueda.TODOS -> auto.modelo.contains(query, ignoreCase = true) ||
                                auto.n_serie.contains(query, ignoreCase = true) ||
                                auto.sku.contains(query, ignoreCase = true) ||
                                auto.color.contains(query, ignoreCase = true)
                    }

                    // Filtrar por disponibilidad
                    val matchesDisponibilidad = disponibilidad == null ||
                            auto.disponibilidad == disponibilidad

                    // Filtrar por rango de precio
                    val matchesPrecioMin = precioMin == null || auto.precio >= precioMin
                    val matchesPrecioMax = precioMax == null || auto.precio <= precioMax

                    // Filtrar por rango de año
                    val matchesAnioMin = anioMin == null || auto.anio >= anioMin
                    val matchesAnioMax = anioMax == null || auto.anio <= anioMax

                    // Todos los filtros deben cumplirse
                    matchesQuery && matchesDisponibilidad &&
                            matchesPrecioMin && matchesPrecioMax &&
                            matchesAnioMin && matchesAnioMax
                }

                // Actualizar el estado de filtros activos
                _hayFiltrosActivos.value = query.isNotEmpty() ||
                        disponibilidad != null ||
                        precioMin != null ||
                        precioMax != null ||
                        anioMin != null ||
                        anioMax != null

                _autos.value = filteredAutos
            } catch (e: Exception) {
                _error.value = "Error al filtrar autos: ${e.message}"
                _autos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Estadísticas del catálogo - útiles para la UI
    val estadisticasCatalogo = repository.autos.map { listaAutos ->
        val totalAutos = listaAutos.size
        val autosDisponibles = listaAutos.count { it.disponibilidad }
        val precioPromedio = if (listaAutos.isNotEmpty()) {
            listaAutos.sumOf { it.precio } / totalAutos
        } else {
            0.0
        }
        val stockTotal = listaAutos.sumOf { it.stock }

        mapOf(
            "totalAutos" to totalAutos,
            "autosDisponibles" to autosDisponibles,
            "precioPromedio" to precioPromedio,
            "stockTotal" to stockTotal
        )
    }

    // Extracción de valores únicos para los filtros
    val coloresDisponibles = repository.autos.map { autos ->
        autos.map { it.color }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val aniosDisponibles = repository.autos.map { autos ->
        autos.map { it.anio }
            .filter { it > 0 }
            .distinct()
            .sorted()
    }

    val rangoPrecio = repository.autos.map { autos ->
        if (autos.isNotEmpty()) {
            val min = autos.minOfOrNull { it.precio } ?: 0.0
            val max = autos.maxOfOrNull { it.precio } ?: 0.0
            Pair(min, max)
        } else {
            Pair(0.0, 0.0)
        }
    }

    // Métodos para actualizar filtros
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTipoBusqueda(tipo: TipoBusqueda) {
        _tipoBusqueda.value = tipo
    }

    // Método adicional para establecer el campo de búsqueda usando un string
    fun setCampoBusqueda(campo: String) {
        _tipoBusqueda.value = when (campo.lowercase()) {
            "modelo" -> TipoBusqueda.MODELO
            "n_serie" -> TipoBusqueda.NUMERO_SERIE
            "sku" -> TipoBusqueda.SKU
            "color" -> TipoBusqueda.COLOR
            "todos" -> TipoBusqueda.TODOS
            else -> TipoBusqueda.MODELO
        }
    }

    fun setDisponibilidadFiltro(disponibilidad: Boolean?) {
        _disponibilidadFiltro.value = disponibilidad
    }

    fun setPrecioMinimo(precio: Double?) {
        _precioMinimo.value = precio
    }

    fun setPrecioMaximo(precio: Double?) {
        _precioMaximo.value = precio
    }

    fun setAnioMinimo(anio: Int?) {
        _anioMinimo.value = anio
    }

    fun setAnioMaximo(anio: Int?) {
        _anioMaximo.value = anio
    }

    fun limpiarFiltros() {
        _searchQuery.value = ""
        _tipoBusqueda.value = TipoBusqueda.MODELO
        _disponibilidadFiltro.value = null
        _precioMinimo.value = null
        _precioMaximo.value = null
        _anioMinimo.value = null
        _anioMaximo.value = null
    }

    // Eliminar un auto
    fun eliminarAuto(autoId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.eliminarAuto(autoId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar el auto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Búsqueda rápida por número de serie o SKU
    fun buscarPorIdentificador(identificador: String) {
        // Intentamos primero con SKU (más corto)
        if (identificador.length <= 10) {
            _searchQuery.value = identificador
            _tipoBusqueda.value = TipoBusqueda.SKU
        } else {
            // Si es más largo, probablemente sea un número de serie
            _searchQuery.value = identificador
            _tipoBusqueda.value = TipoBusqueda.NUMERO_SERIE
        }
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