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

    // LiveData for sales statistics
    private val _estadisticasVentas = MutableLiveData<Map<String, Int>>()
    val estadisticasVentas: LiveData<Map<String, Int>> = _estadisticasVentas

    // Estados válidos para ventas
    companion object {
        // Usar las mismas constantes que en el repositorio
        val ESTADO_PENDIENTE = VentasRepository.ESTADO_PENDIENTE
        val ESTADO_ENTREGADA = VentasRepository.ESTADO_ENTREGADA
        val ESTADO_CANCELADA = VentasRepository.ESTADO_CANCELADA
        val ESTADOS_VALIDOS = VentasRepository.ESTADOS_VALIDOS
    }

    init {
        // Inicializar estadísticas con valores por defecto
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
                // Actualizar estadísticas al obtener todas las ventas
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

        // Validar el estatus antes de pasarlo al repositorio
        val estatusValido = if (estatus in ESTADOS_VALIDOS) {
            estatus
        } else {
            // Si el estatus no es válido, usar PENDIENTE por defecto
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
        // Validar datos de entrada
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
        _error.value = null // Limpiar errores anteriores

        ventasRepository.registrarVenta(nSerie, cantidad, precio) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (ventaObj != null) {
                _venta.value = ventaObj
                _success.value = "Venta registrada exitosamente"
                // Reload the sales list
                obtenerTodasLasVentas()
                // También actualiza estadísticas
                obtenerEstadisticas()
            } else {
                _error.value = "Error: No se pudo registrar la venta"
            }
        }
    }

    fun actualizarEstatus(id: Int, estatus: String) {
        // Validar que el estatus sea uno de los permitidos
        if (estatus !in ESTADOS_VALIDOS) {
            _error.value = "Estado no válido. Debe ser uno de: ${ESTADOS_VALIDOS.joinToString()}"
            return
        }

        _isLoading.value = true
        _error.value = null // Limpiar errores anteriores

        ventasRepository.actualizarEstatus(id, estatus) { ventaObj, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (ventaObj != null) {
                _venta.value = ventaObj
                _success.value = "Estado actualizado exitosamente a: $estatus"

                // Si el estatus se actualizó correctamente, refrescar la lista de ventas
                obtenerTodasLasVentas()
                // También actualiza estadísticas
                obtenerEstadisticas()
            } else {
                _error.value = "Error: No se pudo actualizar el estado"
            }
        }
    }

    fun cancelarVenta(id: Int) {
        _isLoading.value = true
        _error.value = null // Limpiar errores anteriores

        ventasRepository.cancelarVenta(id) { exitoso, errorMsg ->
            _isLoading.value = false
            if (errorMsg != null) {
                _error.value = errorMsg
            } else if (exitoso == true) {
                _success.value = "Venta cancelada exitosamente"
                // Reload the sales list
                obtenerTodasLasVentas()
                // También actualiza estadísticas
                obtenerEstadisticas()
            } else {
                _error.value = "Error: No se pudo cancelar la venta"
            }
        }
    }
    // Obtener estadísticas de ventas desde el servidor
    fun obtenerEstadisticas() {
        ventasRepository.contarVentas { stats, errorMsg ->
            if (errorMsg != null) {
                // Si hay error, calcular estadísticas localmente con las ventas actuales
                val ventasActuales = _ventas.value ?: emptyList()
                actualizarEstadisticasLocales(ventasActuales)
            } else if (stats != null) {
                _estadisticasVentas.value = stats
            }
        }
    }

    // Calcular estadísticas basadas en la lista de ventas en memoria
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

    // Limpiar mensajes de error y éxito
    fun limpiarMensajes() {
        _error.value = null
        _success.value = null
    }

    // Verificar si hay ventas para un auto específico
    fun existenVentasPorNumeroSerie(nSerie: String): Boolean {
        return (_ventas.value ?: emptyList()).any { it.nSerie == nSerie }
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