package com.example.catalogoautos.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AgregarAutoViewModel(private val repository: AutoRepository) : ViewModel() {


    private val _n_serie = MutableLiveData<String>("")
    val n_serie: LiveData<String> = _n_serie

    private val _sku = MutableLiveData<String>("")
    val sku: LiveData<String> = _sku

    private val _modelo = MutableLiveData<String>("")
    val modelo: LiveData<String> = _modelo

    private val _anio = MutableLiveData<Int>(0)
    val anio: LiveData<Int> = _anio

    private val _color = MutableLiveData<String>("")
    val color: LiveData<String> = _color

    private val _precio = MutableLiveData<Double>(0.0)
    val precio: LiveData<Double> = _precio

    private val _stock = MutableLiveData<Int>(0)
    val stock: LiveData<Int> = _stock

    private val _descripcion = MutableLiveData<String>("")
    val descripcion: LiveData<String> = _descripcion

    private val _disponibilidad = MutableLiveData<Boolean>(true)
    val disponibilidad: LiveData<Boolean> = _disponibilidad


    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage


    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading


    private var autoActual: Auto = Auto(
        auto_id = 0,
        n_serie = "",
        sku = "",
        marca_id = 1,
        modelo = "",
        anio = 0,
        color = "",
        precio = 0.0,
        stock = 0,
        descripcion = "",
        disponibilidad = true,
        fecha_registro = LocalDateTime.now(),
        fecha_actualizacion = LocalDateTime.now()
    )


    fun setAutoParaEditar(autoId: Int?) {
        if (autoId != null && autoId > 0) {
            val auto = repository.obtenerAutoPorId(autoId)
            if (auto != null) {
                autoActual = auto


                _n_serie.value = auto.n_serie
                _sku.value = auto.sku
                _modelo.value = auto.modelo
                _anio.value = auto.anio
                _color.value = auto.color
                _precio.value = auto.precio
                _stock.value = auto.stock
                _descripcion.value = auto.descripcion
                _disponibilidad.value = auto.disponibilidad
            }
        }
    }


    fun getAutoActual(): Auto {
        return autoActual
    }


    fun actualizarNSerie(n_serie: String) {
        _n_serie.value = n_serie
    }

    fun actualizarSku(sku: String) {
        _sku.value = sku
    }

    fun actualizarModelo(modelo: String) {
        _modelo.value = modelo
    }

    fun actualizarAnio(anio: Int) {
        _anio.value = anio
    }

    fun actualizarColor(color: String) {
        _color.value = color
    }

    fun actualizarPrecio(precio: Double) {
        _precio.value = precio
    }

    fun actualizarStock(stock: Int) {
        _stock.value = stock
    }

    fun actualizarDescripcion(descripcion: String) {
        _descripcion.value = descripcion
    }

    fun actualizarDisponibilidad(disponibilidad: Boolean) {
        _disponibilidad.value = disponibilidad
    }


    fun validarCampos(): Boolean {
        if (_n_serie.value.isNullOrBlank()) {
            _errorMessage.value = "El número de serie es obligatorio"
            return false
        }

        if (_sku.value.isNullOrBlank()) {
            _errorMessage.value = "El SKU es obligatorio"
            return false
        }

        if (_sku.value?.length ?: 0 > 10) {
            _errorMessage.value = "El SKU no puede tener más de 10 caracteres"
            return false
        }

        if (_modelo.value.isNullOrBlank()) {
            _errorMessage.value = "El modelo es obligatorio"
            return false
        }

        if (_anio.value ?: 0 <= 1900) {
            _errorMessage.value = "El año debe ser mayor a 1900"
            return false
        }

        if (_precio.value ?: 0.0 < 0) {
            _errorMessage.value = "El precio no puede ser negativo"
            return false
        }

        if (_stock.value ?: 0 < 0) {
            _errorMessage.value = "El stock no puede ser negativo"
            return false
        }

        _errorMessage.value = null
        return true
    }


    suspend fun guardarAutoRemoto(auto: Auto): Result<Auto> {
        _isLoading.value = true
        try {
            return if (auto.auto_id == 0) {

                repository.agregarAutoRemoto(auto)
            } else {

                repository.actualizarAutoRemoto(auto)
            }
        } finally {
            _isLoading.value = false
        }
    }


    fun guardarAuto(auto: Auto) {
        _isLoading.value = true
        viewModelScope.launch {
            try {

                val resultado = guardarAutoRemoto(auto)
                resultado.fold(
                    onSuccess = {
                        _errorMessage.value = null
                    },
                    onFailure = { error ->

                        _errorMessage.value = "Error al guardar en el servidor: ${error.message}. " +
                                "Los datos se han guardado localmente."
                    }
                )
            } catch (e: Exception) {

                if (auto.auto_id == 0) {
                    // Es un nuevo auto
                    repository.agregarAuto(auto)
                } else {
                    // Es un auto existente
                    repository.actualizarAuto(auto)
                }
                _errorMessage.value = "Error: ${e.message}. Los datos se han guardado localmente."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun guardarAuto() {
        if (!validarCampos()) {
            return
        }
        val autoAGuardar = Auto(
            auto_id = autoActual.auto_id,
            n_serie = _n_serie.value ?: "",
            sku = _sku.value ?: "",
            marca_id = 1, // BYD por defecto
            modelo = _modelo.value ?: "",
            anio = _anio.value ?: 0,
            color = _color.value ?: "",
            precio = _precio.value ?: 0.0,
            stock = _stock.value ?: 0,
            descripcion = _descripcion.value ?: "",
            disponibilidad = _disponibilidad.value ?: true,
            fecha_registro = autoActual.fecha_registro,
            fecha_actualizacion = LocalDateTime.now()
        )

        guardarAuto(autoAGuardar)
    }


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