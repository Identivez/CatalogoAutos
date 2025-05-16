package com.example.catalogoautos.viewmodel

import Venta
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.catalogoautos.repository.VentasRepository
import java.time.LocalDateTime

class ReportesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VentasRepository(application.applicationContext)
    private val _ventas = MutableLiveData<List<Venta>>()
    val ventas: LiveData<List<Venta>> get() = _ventas

    fun cargarVentas(fechaInicio: LocalDateTime, fechaFin: LocalDateTime, estatus: String?) {
        repository.getVentasPorFiltro(fechaInicio, fechaFin, estatus, object : VentasRepository.VentasCallback {
            override fun onVentasRecibidas(resultado: List<Venta>) {
                _ventas.postValue(resultado)
            }

            override fun onError(mensaje: String) {
                // Manejar error
                _ventas.postValue(emptyList())
            }
        })
    }

    // Factory para proveer el Application al ViewModel
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReportesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReportesViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}