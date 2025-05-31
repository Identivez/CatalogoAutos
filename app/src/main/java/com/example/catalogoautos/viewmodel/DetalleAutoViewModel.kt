package com.example.catalogoautos.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DetalleAutoViewModel(private val repository: AutoRepository) : ViewModel() {


    private val _auto = MutableLiveData<Auto?>(null)
    val auto: LiveData<Auto?> = _auto


    private val _precio = MutableLiveData<String>("")
    val precio: LiveData<String> = _precio

    private val _fechaRegistro = MutableLiveData<String>("")
    val fechaRegistro: LiveData<String> = _fechaRegistro

    private val _fechaActualizacion = MutableLiveData<String>("")
    val fechaActualizacion: LiveData<String> = _fechaActualizacion

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error


    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()


    private val serverUrl = "http://192.168.1.18:8080/ae_byd/api/auto"


    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")


    fun cargarAuto(autoId: Int) {
        _isLoading.value = true
        _error.value = null

        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/$autoId")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)


                    val auto = Auto(
                        auto_id = jsonObject.getInt("auto_id"),
                        n_serie = jsonObject.getString("n_serie"),
                        sku = jsonObject.getString("sku"),
                        marca_id = jsonObject.getInt("marca_id"),
                        modelo = jsonObject.getString("modelo"),
                        anio = jsonObject.getInt("anio"),
                        color = jsonObject.getString("color"),
                        precio = jsonObject.getDouble("precio"),
                        stock = jsonObject.getInt("stock"),
                        descripcion = jsonObject.optString("descripcion", ""),
                        disponibilidad = jsonObject.getBoolean("disponibilidad"),
                        fecha_registro = parseFechaFromString(jsonObject.getString("fecha_registro")),
                        fecha_actualizacion = parseFechaFromString(jsonObject.getString("fecha_actualizacion"))
                    )


                    _auto.postValue(auto)
                    _precio.postValue(formatearPrecio(auto.precio))
                    _fechaRegistro.postValue(formatearFecha(auto.fecha_registro))
                    _fechaActualizacion.postValue(formatearFecha(auto.fecha_actualizacion))


                    try {
                        repository.actualizarAuto(auto)
                    } catch (e: Exception) {
                        Log.e("DetalleAutoViewModel", "Error al actualizar copia local: ${e.message}")
                    }
                } else {

                    Log.e("DetalleAutoViewModel", "Error del servidor: ${response.code}")

                    // Intentar obtener desde repositorio local
                    val autoLocal = repository.obtenerAutoPorId(autoId)
                    if (autoLocal != null) {
                        _auto.postValue(autoLocal)
                        _precio.postValue(formatearPrecio(autoLocal.precio))
                        _fechaRegistro.postValue(formatearFecha(autoLocal.fecha_registro))
                        _fechaActualizacion.postValue(formatearFecha(autoLocal.fecha_actualizacion))
                    } else {
                        _error.postValue("Auto no encontrado")
                    }
                }
            } catch (e: Exception) {
                Log.e("DetalleAutoViewModel", "Error al cargar auto: ${e.message}")


                try {
                    val autoLocal = repository.obtenerAutoPorId(autoId)
                    if (autoLocal != null) {
                        _auto.postValue(autoLocal)
                        _precio.postValue(formatearPrecio(autoLocal.precio))
                        _fechaRegistro.postValue(formatearFecha(autoLocal.fecha_registro))
                        _fechaActualizacion.postValue(formatearFecha(autoLocal.fecha_actualizacion))
                    } else {
                        _error.postValue("Auto no encontrado")
                    }
                } catch (ex: Exception) {
                    _error.postValue("Error al cargar auto: ${ex.message}")
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }


    fun actualizarDisponibilidad(autoId: Int, disponible: Boolean) {
        _isLoading.value = true
        _error.value = null

        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/$autoId/disponibilidad/$disponible")
                    .put("".toRequestBody(null))
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {

                    val autoActual = _auto.value
                    if (autoActual != null) {
                        val autoActualizado = autoActual.copy(
                            disponibilidad = disponible,
                            fecha_actualizacion = LocalDateTime.now()
                        )

                        _auto.postValue(autoActualizado)
                        _fechaActualizacion.postValue(formatearFecha(autoActualizado.fecha_actualizacion))


                        try {
                            repository.actualizarAuto(autoActualizado)
                        } catch (e: Exception) {
                            Log.e("DetalleAutoViewModel", "Error al actualizar copia local: ${e.message}")
                        }
                    }


                    cargarAuto(autoId)
                } else {
                    _error.postValue("Error al actualizar disponibilidad: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("DetalleAutoViewModel", "Error al actualizar disponibilidad: ${e.message}")
                _error.postValue("Error al actualizar disponibilidad: ${e.message}")

                try {
                    val autoActual = _auto.value
                    if (autoActual != null) {
                        val autoActualizado = autoActual.copy(
                            disponibilidad = disponible,
                            fecha_actualizacion = LocalDateTime.now()
                        )

                        _auto.postValue(autoActualizado)
                        _fechaActualizacion.postValue(formatearFecha(autoActualizado.fecha_actualizacion))
                        repository.actualizarAuto(autoActualizado)
                    }
                } catch (ex: Exception) {
                    Log.e("DetalleAutoViewModel", "Error actualizando localmente: ${ex.message}")
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }


    private fun parseFechaFromString(fechaStr: String): LocalDateTime {
        return try {

            if (fechaStr.contains("T")) {
                LocalDateTime.parse(fechaStr)
            } else {

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                LocalDateTime.parse(fechaStr, formatter)
            }
        } catch (e: Exception) {
            Log.e("DetalleAutoViewModel", "Error al parsear fecha: $fechaStr - ${e.message}")
            LocalDateTime.now()
        }
    }

    private fun formatearPrecio(precio: Double): String {
        return "$${String.format("%,.2f", precio)}"
    }

    private fun formatearFecha(fecha: LocalDateTime): String {
        return fecha.format(dateFormatter)
    }


    fun hayStockDisponible(): Boolean {
        return _auto.value?.stock ?: 0 > 0
    }


    fun debeMostrarIndicadorNuevoCampo(campo: String): Boolean {

        return campo == "n_serie" || campo == "sku"
    }

    class Factory(private val repository: AutoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetalleAutoViewModel::class.java)) {
                return DetalleAutoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}