package com.example.catalogoautos.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository

class AgregarAutoViewModel(private val autoRepository: AutoRepository) : ViewModel() {

    // Auto actual que estamos creando o editando
    private var autoActual: Auto? = null

    // Método para configurar el auto a editar basado en su ID
    fun setAutoParaEditar(id: String?) {
        if (id != null) {
            autoActual = autoRepository.obtenerAutoPorId(id)
        } else {
            autoActual = Auto() // Crear un nuevo auto vacío
        }
    }

    // Método para obtener el auto actual
    fun getAutoActual(): Auto {
        return autoActual ?: Auto()
    }

    // Método para guardar o actualizar un auto en el repositorio
    fun guardarAuto(auto: Auto) {
        Log.d("AgregarAutoViewModel", "Guardando auto. ID actual: ${autoActual?.id}, Nuevo ID: ${auto.id}")
        val existingAuto = auto.id?.let { autoRepository.obtenerAutoPorId(it) }

        if (existingAuto != null) {
            Log.d("AgregarAutoViewModel", "Auto existente encontrado. Actualizando.")
            autoRepository.actualizarAuto(auto)
        } else {
            Log.d("AgregarAutoViewModel", "Auto no encontrado. Agregando nuevo.")
            autoRepository.agregarAuto(auto)
        }
    }

    // Factory para crear instancias de este ViewModel con sus dependencias
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AgregarAutoViewModel::class.java)) {
                return AgregarAutoViewModel(AutoRepository.getInstance(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}