package com.example.catalogoautos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import java.util.*

class AgregarAutoViewModel(private val repository: AutoRepository) : ViewModel() {

    // Auto que se está editando o un nuevo auto en caso de inserción
    private var autoActual: Auto = Auto(
        auto_id = 0,
        marca_id = 0,
        modelo = "",
        año = 0,
        color = "",
        precio = 0.0,
        stock = 0,
        descripcion = "",
        disponibilidad = true,
        fecha_registro = Date(),
        fecha_actualizacion = Date()
    )

    // Establece el auto que se está editando basado en su ID
    fun setAutoParaEditar(autoId: Int?) {
        if (autoId != null && autoId > 0) {
            val auto = repository.obtenerAutoPorId(autoId)
            if (auto != null) {
                autoActual = auto
            }
        }
    }

    // Retorna el auto que se está editando o el auto vacío en caso de inserción
    fun getAutoActual(): Auto {
        return autoActual
    }

    // Guarda el auto en el repositorio (inserción o actualización)
    fun guardarAuto(auto: Auto) {
        if (auto.auto_id == 0) {
            // Es un nuevo auto
            repository.agregarAuto(auto)
        } else {
            // Es un auto existente
            repository.actualizarAuto(auto)
        }
    }

    // Factory para crear el ViewModel con el repositorio
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