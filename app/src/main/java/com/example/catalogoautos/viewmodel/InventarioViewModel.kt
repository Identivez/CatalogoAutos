package com.example.catalogoautos.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class InventarioViewModel(private val autoRepository: AutoRepository) : ViewModel() {

    // Enumeración para definir el tipo de búsqueda
    enum class TipoBusqueda {
        MODELO, NUMERO_SERIE, SKU, TODOS
    }

    // Término de búsqueda
    private val _searchQuery = MutableStateFlow("")

    // Tipo de búsqueda actual
    private val _tipoBusqueda = MutableStateFlow(TipoBusqueda.TODOS)

    // Solo mostrar disponibles
    private val _soloDisponibles = MutableStateFlow(false)

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Nombre fijo de la marca - ahora solo tenemos BYD
    private val marcaNombre = "BYD"

    // Formateador para fechas
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Autos filtrados según el término de búsqueda
    val autos: StateFlow<List<Auto>> = combine(
        autoRepository.autos,
        _searchQuery,
        _tipoBusqueda,
        _soloDisponibles
    ) { listaAutos, query, tipo, soloDisponibles ->
        Log.d("InventarioViewModel", "Combinando flujos - Lista original: ${listaAutos.size} autos, Query: '$query', Tipo: $tipo, Solo Disponibles: $soloDisponibles")

        _isLoading.value = true

        try {
            val resultado = listaAutos.filter { auto ->
                // Primero filtrar por disponibilidad si es necesario
                val cumpleDisponibilidad = !soloDisponibles || auto.disponibilidad

                // Luego filtrar por el término de búsqueda según el tipo seleccionado
                val cumpleBusqueda = if (query.isBlank()) {
                    true // Si no hay query, cumple la búsqueda
                } else {
                    when (tipo) {
                        TipoBusqueda.MODELO -> auto.modelo.contains(query, ignoreCase = true)
                        TipoBusqueda.NUMERO_SERIE -> auto.n_serie.contains(query, ignoreCase = true)
                        TipoBusqueda.SKU -> auto.sku.contains(query, ignoreCase = true)
                        TipoBusqueda.TODOS ->
                            auto.modelo.contains(query, ignoreCase = true) ||
                                    auto.n_serie.contains(query, ignoreCase = true) ||
                                    auto.sku.contains(query, ignoreCase = true) ||
                                    auto.color.contains(query, ignoreCase = true) ||
                                    auto.anio.toString() == query
                    }
                }

                cumpleDisponibilidad && cumpleBusqueda
            }

            Log.d("InventarioViewModel", "Resultado filtrado: ${resultado.size} autos")
            _error.value = null
            resultado
        } catch (e: Exception) {
            Log.e("InventarioViewModel", "Error al filtrar autos", e)
            _error.value = "Error al filtrar autos: ${e.message}"
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Estadísticas de inventario
    val estadisticasInventario = autoRepository.autos.map { listaAutos ->
        val totalAutos = listaAutos.size
        val autosDisponibles = listaAutos.count { it.disponibilidad }
        val stockTotal = listaAutos.sumOf { it.stock }
        val valorInventario = listaAutos.sumOf { it.precio * it.stock }

        mapOf(
            "totalAutos" to totalAutos,
            "autosDisponibles" to autosDisponibles,
            "stockTotal" to stockTotal,
            "valorInventario" to valorInventario
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        mapOf(
            "totalAutos" to 0,
            "autosDisponibles" to 0,
            "stockTotal" to 0,
            "valorInventario" to 0.0
        )
    )

    init {
        // Registro de diagnóstico para verificar que el ViewModel se está inicializando correctamente
        Log.d("InventarioViewModel", "ViewModel inicializado")
    }

    // Método para actualizar el término de búsqueda
    fun setSearchQuery(query: String) {
        Log.d("InventarioViewModel", "Actualizando query de búsqueda a: '$query'")
        _searchQuery.value = query
    }

    // Método para cambiar el tipo de búsqueda
    fun setTipoBusqueda(tipo: TipoBusqueda) {
        Log.d("InventarioViewModel", "Cambiando tipo de búsqueda a: $tipo")
        _tipoBusqueda.value = tipo
    }

    // Método para filtrar solo disponibles
    fun setSoloDisponibles(soloDisponibles: Boolean) {
        Log.d("InventarioViewModel", "Filtrando solo disponibles: $soloDisponibles")
        _soloDisponibles.value = soloDisponibles
    }

    // Limpiar filtros
    fun limpiarFiltros() {
        Log.d("InventarioViewModel", "Limpiando todos los filtros")
        _searchQuery.value = ""
        _tipoBusqueda.value = TipoBusqueda.TODOS
        _soloDisponibles.value = false
    }

    // Obtener auto por ID
    fun obtenerAutoPorId(id: Int): Auto? {
        val auto = autoRepository.obtenerAutoPorId(id)
        Log.d("InventarioViewModel", "Buscando auto ID: $id - Encontrado: ${auto != null}")
        return auto
    }

    // Método para buscar por código de barras escaneado
    // (Asumimos que podría ser un número de serie o SKU)
    fun buscarPorCodigoEscaneado(codigo: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("InventarioViewModel", "Buscando código escaneado: $codigo")
                if (codigo.length <= 10) {
                    // Probablemente es un SKU
                    _searchQuery.value = codigo
                    _tipoBusqueda.value = TipoBusqueda.SKU
                } else {
                    // Probablemente es un número de serie
                    _searchQuery.value = codigo
                    _tipoBusqueda.value = TipoBusqueda.NUMERO_SERIE
                }
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error al buscar código", e)
                _error.value = "Error al buscar código: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para actualizar el stock de un auto
    fun actualizarStock(autoId: Int, nuevoStock: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auto = autoRepository.obtenerAutoPorId(autoId)
                auto?.let {
                    val autoActualizado = it.copy(stock = nuevoStock)
                    autoRepository.actualizarAuto(autoActualizado)
                    Log.d("InventarioViewModel", "Stock actualizado para auto ID: $autoId, Nuevo stock: $nuevoStock")
                }
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error al actualizar stock", e)
                _error.value = "Error al actualizar stock: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para forzar la carga de autos desde la base de datos
    fun cargarAutos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("InventarioViewModel", "Solicitando recarga de autos al repositorio")
                // Si tu repository tiene un método para recargar, llámalo aquí
                // autoRepository.recargarAutos()
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error al recargar autos", e)
                _error.value = "Error al recargar autos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventarioViewModel::class.java)) {
                Log.d("InventarioViewModel.Factory", "Creando ViewModel con contexto: $context")
                return InventarioViewModel(AutoRepository.getInstance(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}