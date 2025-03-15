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
import kotlinx.coroutines.flow.stateIn

class InventarioViewModel(private val autoRepository: AutoRepository) : ViewModel() {

    // Término de búsqueda
    private val _searchQuery = MutableStateFlow("")

    // Autos filtrados según el término de búsqueda
    val autos: StateFlow<List<Auto>> = combine(
        autoRepository.autos,
        _searchQuery
    ) { listaAutos, query ->
        Log.d("InventarioViewModel", "Combinando flujos - Lista original: ${listaAutos.size} autos, Query: '$query'")

        val resultado = if (query.isBlank()) {
            listaAutos
        } else {
            listaAutos.filter {
                it.marca.contains(query, ignoreCase = true) ||
                        it.modelo.contains(query, ignoreCase = true)
            }
        }

        Log.d("InventarioViewModel", "Resultado filtrado: ${resultado.size} autos")
        resultado
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
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

    // Obtener auto por ID
    fun obtenerAutoPorId(id: String): Auto? {
        val auto = autoRepository.obtenerAutoPorId(id)
        Log.d("InventarioViewModel", "Buscando auto ID: $id - Encontrado: ${auto != null}")
        return auto
    }

    // Método para forzar la carga de autos desde SharedPreferences si es necesario
    fun cargarAutos() {
        // Si tu repositorio tiene un método para recargar datos, llámalo aquí
        Log.d("InventarioViewModel", "Solicitando recarga de autos al repositorio")
        // autoRepository.recargarAutos() // Implementa este método si es necesario
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