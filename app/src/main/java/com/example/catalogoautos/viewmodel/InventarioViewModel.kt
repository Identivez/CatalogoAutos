package com.example.catalogoautos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class InventarioViewModel(private val repository: AutoRepository) : ViewModel() {


    enum class TipoBusqueda {
        MODELO, NUMERO_SERIE, SKU, TODOS
    }


    private val _autos = MutableStateFlow<List<Auto>>(emptyList())
    val autos: StateFlow<List<Auto>> = _autos.asStateFlow()


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    private val _tipoBusqueda = MutableStateFlow(TipoBusqueda.TODOS)
    val tipoBusqueda: StateFlow<TipoBusqueda> = _tipoBusqueda.asStateFlow()


    private val _soloDisponibles = MutableStateFlow(false)
    val soloDisponibles: StateFlow<Boolean> = _soloDisponibles.asStateFlow()


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    private val _estadisticasInventario = MutableStateFlow(mapOf(
        "totalAutos" to 0,
        "autosDisponibles" to 0,
        "stockTotal" to 0
    ))
    val estadisticasInventario: StateFlow<Map<String, Int>> = _estadisticasInventario.asStateFlow()


    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()


    private var serverUrl = "http://192.168.1.18:8080/ae_byd/api/auto"

    init {

        cargarAutos()
    }


    private fun verificarConexionServidor() {
        viewModelScope.launch {

            val urlsParaProbar = listOf(
                "http://192.168.1.18:8080/ae_byd/api/auto",
                "http://192.168.1.18:8080/AE_BYD/api/auto",
                "http://192.168.1.18:8080/ae_byd-1.0-SNAPSHOT/api/auto",
                "http://192.168.1.18:8080/AE_BYD-1.0-SNAPSHOT/api/auto",
                // Probar también con localhost
                "http://localhost:8080/ae_byd/api/auto",
                "http://localhost:8080/AE_BYD/api/auto"
            )

            Log.d("InventarioViewModel", "Intentando verificar conexión con el servidor...")

            for (url in urlsParaProbar) {
                try {
                    Log.d("InventarioViewModel", "Probando URL: $url")
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()

                    Log.d("InventarioViewModel", "Respuesta de $url: código ${response.code}")

                    if (response.isSuccessful) {
                        Log.d("InventarioViewModel", "¡URL exitosa!: $url")
                        // Actualizar la URL del servidor
                        serverUrl = url
                        Log.d("InventarioViewModel", "Conexión establecida con: $url")
                        break
                    }
                } catch (e: Exception) {
                    Log.e("InventarioViewModel", "Error al probar URL $url: ${e.message}")
                }
            }

            Log.d("InventarioViewModel", "URL final seleccionada: $serverUrl")
        }
    }


    fun cargarAutos() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {

                Log.d("InventarioViewModel", "Intentando cargar autos desde el servidor...")
                verificarConexionServidor()

                val request = Request.Builder()
                    .url(serverUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    Log.d("InventarioViewModel", "Datos recibidos del servidor")
                    val jsonArray = JSONArray(responseBody)
                    val autos = mutableListOf<Auto>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)

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

                        autos.add(auto)
                        Log.d("InventarioViewModel", "Auto cargado: ${auto.modelo} - ${auto.sku}")
                    }


                    actualizarRepositorioLocal(autos)


                    _autos.value = autos


                    actualizarEstadisticas(autos)
                } else {

                    Log.e("InventarioViewModel", "Error del servidor: ${response.code}")
                    cargarAutosLocales()
                }
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error de conexión: ${e.message}")

                cargarAutosLocales()
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun cargarAutosLocales() {
        try {
            Log.d("InventarioViewModel", "Cargando autos desde repositorio local...")
            val autosLocales = repository.obtenerTodosLosAutos()
            _autos.value = autosLocales
            actualizarEstadisticas(autosLocales)

            Log.d("InventarioViewModel", "Autos cargados localmente: ${autosLocales.size}")
            autosLocales.forEach {
                Log.d("InventarioViewModel", "Auto local: ${it.modelo} - ${it.sku}")
            }

            if (autosLocales.isEmpty()) {
                _error.value = "No hay autos registrados"
            }
        } catch (e: Exception) {
            Log.e("InventarioViewModel", "Error al cargar autos locales: ${e.message}")
            _error.value = "Error al cargar autos: ${e.message}"
        }
    }


    private fun actualizarRepositorioLocal(autos: List<Auto>) {
        autos.forEach { auto ->
            try {

                val autoExistente = repository.obtenerAutoPorId(auto.auto_id)
                if (autoExistente != null) {
                    repository.actualizarAuto(auto)
                    Log.d("InventarioViewModel", "Auto actualizado localmente: ${auto.modelo} - ${auto.sku}")
                } else {

                    repository.agregarAuto(auto)
                    Log.d("InventarioViewModel", "Auto agregado localmente: ${auto.modelo} - ${auto.sku}")
                }
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error al sincronizar auto local: ${e.message}")
            }
        }
    }


    private fun actualizarEstadisticas(autos: List<Auto>) {
        val totalAutos = autos.size
        val autosDisponibles = autos.count { it.disponibilidad }
        val stockTotal = autos.sumOf { it.stock }

        _estadisticasInventario.value = mapOf(
            "totalAutos" to totalAutos,
            "autosDisponibles" to autosDisponibles,
            "stockTotal" to stockTotal
        )
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
            Log.e("InventarioViewModel", "Error al parsear fecha: $fechaStr - ${e.message}")
            LocalDateTime.now()
        }
    }


    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filtrarAutos()
    }

    fun setTipoBusqueda(tipo: TipoBusqueda) {
        _tipoBusqueda.value = tipo
        filtrarAutos()
    }

    fun setSoloDisponibles(soloDisponibles: Boolean) {
        _soloDisponibles.value = soloDisponibles
        filtrarAutos()
    }

    private fun filtrarAutos() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val autosOriginales = repository.obtenerTodosLosAutos()
                val query = _searchQuery.value
                val tipo = _tipoBusqueda.value
                val soloDisponibles = _soloDisponibles.value

                val autosFiltrados = autosOriginales.filter { auto ->

                    val cumpleDisponibilidad = !soloDisponibles || auto.disponibilidad


                    val cumpleBusqueda = if (query.isBlank()) {
                        true
                    } else {
                        when (tipo) {
                            TipoBusqueda.MODELO -> auto.modelo.contains(query, ignoreCase = true)
                            TipoBusqueda.NUMERO_SERIE -> auto.n_serie.contains(query, ignoreCase = true)
                            TipoBusqueda.SKU -> auto.sku.contains(query, ignoreCase = true)
                            TipoBusqueda.TODOS ->
                                auto.modelo.contains(query, ignoreCase = true) ||
                                        auto.n_serie.contains(query, ignoreCase = true) ||
                                        auto.sku.contains(query, ignoreCase = true) ||
                                        auto.color.contains(query, ignoreCase = true) ||
                                        auto.anio.toString() == query
                        }
                    }

                    cumpleDisponibilidad && cumpleBusqueda
                }

                _autos.value = autosFiltrados
                actualizarEstadisticas(autosFiltrados)
            } catch (e: Exception) {
                Log.e("InventarioViewModel", "Error al filtrar autos: ${e.message}")
                _error.value = "Error al filtrar autos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    class Factory(private val repository: AutoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventarioViewModel::class.java)) {
                return InventarioViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}