package com.example.catalogoautos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.model.Venta
import com.example.catalogoautos.repository.VentasRepository
import java.math.BigDecimal

class VentasViewModel(application: Application) : AndroidViewModel(application) {

    // Use lazy initialization to ensure repository is non-null
    private val ventasRepository: VentasRepository by lazy {
        VentasRepository.getInstance(application.applicationContext)
    }

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for list of sales
    private val _ventas = MutableLiveData<List<Venta>>()
    val ventas: LiveData<List<Venta>> = _ventas

    // LiveData for a specific sale
    private val _venta = MutableLiveData<Venta?>()
    val venta: LiveData<Venta?> = _venta

    // LiveData for error messages
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData for success messages
    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    fun obtenerTodasLasVentas() {
        _isLoading.value = true
        ventasRepository.getAllVentas { ventasList, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else {
                _ventas.value = ventasList ?: emptyList()
            }
        }
    }

    fun obtenerVentaPorId(id: Int) {
        _isLoading.value = true
        ventasRepository.getVentaById(id) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
                _venta.value = null
            } else {
                _venta.value = ventaObj
            }
        }
    }

    fun obtenerVentasPorEstatus(estatus: String) {
        _isLoading.value = true
        ventasRepository.getVentasByEstatus(estatus) { ventasList, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else {
                _ventas.value = ventasList ?: emptyList()
            }
        }
    }

    fun registrarVenta(nSerie: String, cantidad: Int, precio: BigDecimal) {
        _isLoading.value = true
        ventasRepository.registrarVenta(nSerie, cantidad, precio) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (ventaObj != null) {
                _venta.value = ventaObj
                _success.value = "Venta registrada exitosamente"
                // Reload the sales list
                obtenerTodasLasVentas()
            } else {
                _error.value = "Error: No se pudo registrar la venta"
            }
        }
    }

    fun actualizarEstatus(id: Int, estatus: String) {
        _isLoading.value = true
        ventasRepository.actualizarEstatus(id, estatus) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (ventaObj != null) {
                _venta.value = ventaObj
                _success.value = "Estado actualizado exitosamente"
            } else {
                _error.value = "Error: No se pudo actualizar el estado"
            }
        }
    }

    fun cancelarVenta(id: Int) {
        _isLoading.value = true
        ventasRepository.cancelarVenta(id) { exitoso, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (exitoso == true) {
                _success.value = "Venta cancelada exitosamente"
                // Reload the sales list
                obtenerTodasLasVentas()
            } else {
                _error.value = "Error: No se pudo cancelar la venta"
            }
        }
    }

    // Factory for creating the ViewModel
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VentasViewModel::class.java)) {
                return VentasViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}