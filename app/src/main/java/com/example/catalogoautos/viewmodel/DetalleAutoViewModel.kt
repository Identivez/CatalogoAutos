package com.example.catalogoautos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository

class DetalleAutoViewModel(private val repository: AutoRepository) : ViewModel() {

    /**
     * Obtiene un auto por su ID desde el repositorio
     */
    fun obtenerAuto(autoId: Int): Auto? {
        return repository.obtenerAutoPorId(autoId)
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