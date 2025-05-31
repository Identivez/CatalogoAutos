package com.example.catalogoautos.viewmodel

import Venta
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.catalogoautos.repository.VentasRepository
import java.math.BigDecimal

class VentasViewModel(application: Application) : AndroidViewModel(application) {


    private val ventasRepository: VentasRepository by lazy {
        VentasRepository.getInstance(application.applicationContext)
    }


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    private val _ventas = MutableLiveData<List<Venta>>()
    val ventas: LiveData<List<Venta>> = _ventas


    private val _venta = MutableLiveData<Venta?>()
    val venta: LiveData<Venta?> = _venta


    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success


    private val _estadisticasVentas = MutableLiveData<Map<String, Int>>()
    val estadisticasVentas: LiveData<Map<String, Int>> = _estadisticasVentas


    companion object {

        val ESTADO_PENDIENTE = VentasRepository.ESTADO_PENDIENTE
        val ESTADO_ENTREGADA = VentasRepository.ESTADO_ENTREGADA
        val ESTADO_CANCELADA = VentasRepository.ESTADO_CANCELADA
        val ESTADOS_VALIDOS = VentasRepository.ESTADOS_VALIDOS
    }

    init {

        _estadisticasVentas.value = mapOf(
            "total" to 0,
            "pendientes" to 0,
            "entregadas" to 0,
            "canceladas" to 0
        )
    }

    fun obtenerTodasLasVentas() {
        _isLoading.value = true
        ventasRepository.getAllVentas { ventasList, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else {
                _ventas.value = ventasList ?: emptyList()

                actualizarEstadisticasLocales(ventasList ?: emptyList())
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


        val estatusValido = if (estatus in ESTADOS_VALIDOS) {
            estatus
        } else {

            ESTADO_PENDIENTE
        }

        ventasRepository.getVentasByEstatus(estatusValido) { ventasList, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else {
                _ventas.value = ventasList ?: emptyList()
            }
        }
    }

    fun registrarVenta(nSerie: String, cantidad: Int, precio: BigDecimal) {

        if (nSerie.isBlank()) {
            _error.value = "El número de serie es obligatorio"
            return
        }

        if (cantidad <= 0) {
            _error.value = "La cantidad debe ser mayor a cero"
            return
        }

        if (precio <= BigDecimal.ZERO) {
            _error.value = "El precio debe ser mayor a cero"
            return
        }

        _isLoading.value = true
        _error.value = null

        ventasRepository.registrarVenta(nSerie, cantidad, precio) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (ventaObj != null) {
                _venta.value = ventaObj
                _success.value = "Venta registrada exitosamente"

                obtenerTodasLasVentas()

                obtenerEstadisticas()
            } else {
                _error.value = "Error: No se pudo registrar la venta"
            }
        }
    }

    fun actualizarEstatus(id: Int, estatus: String) {

        if (estatus !in ESTADOS_VALIDOS) {
            _error.value = "Estado no válido. Debe ser uno de: ${ESTADOS_VALIDOS.joinToString()}"
            return
        }

        _isLoading.value = true
        _error.value = null

        ventasRepository.actualizarEstatus(id, estatus) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (ventaObj != null) {
                _venta.value = ventaObj
                _success.value = "Estado actualizado exitosamente a: $estatus"


                obtenerTodasLasVentas()

                obtenerEstadisticas()
            } else {
                _error.value = "Error: No se pudo actualizar el estado"
            }
        }
    }

    fun cancelarVenta(id: Int) {
        _isLoading.value = true
        _error.value = null

        ventasRepository.cancelarVenta(id) { exitoso, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (exitoso == true) {
                _success.value = "Venta cancelada exitosamente"

                obtenerTodasLasVentas()

                obtenerEstadisticas()
            } else {
                _error.value = "Error: No se pudo cancelar la venta"
            }
        }
    }

    fun obtenerEstadisticas() {
        ventasRepository.contarVentas { stats, errorMsg ->
            if (errorMsg != null) {

                val ventasActuales = _ventas.value ?: emptyList()
                actualizarEstadisticasLocales(ventasActuales)
            } else if (stats != null) {
                _estadisticasVentas.value = stats
            }
        }
    }


    private fun actualizarEstadisticasLocales(ventas: List<Venta>) {
        val total = ventas.size
        val pendientes = ventas.count { it.estatus == ESTADO_PENDIENTE }
        val entregadas = ventas.count { it.estatus == ESTADO_ENTREGADA }
        val canceladas = ventas.count { it.estatus == ESTADO_CANCELADA }

        _estadisticasVentas.value = mapOf(
            "total" to total,
            "pendientes" to pendientes,
            "entregadas" to entregadas,
            "canceladas" to canceladas
        )
    }


    fun limpiarMensajes() {
        _error.value = null
        _success.value = null
    }


    fun existenVentasPorNumeroSerie(nSerie: String): Boolean {
        return (_ventas.value ?: emptyList()).any { it.nSerie == nSerie }
    }


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