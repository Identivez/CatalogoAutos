package com.example.catalogoautos.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.network.ApiClient
import com.example.catalogoautos.network.AutoMap
import com.example.catalogoautos.network.NetworkUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AutoRepository(private val context: Context) {

    private val TAG = "AutoRepository"


    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
        .create()

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "autos_preferences", Context.MODE_PRIVATE
    )


    private val _autos = MutableStateFlow<List<Auto>>(emptyList())


    val autos: Flow<List<Auto>> = _autos.asStateFlow()

    init {

        cargarAutosGuardados()
    }


    private class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }
    }


    private class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LocalDateTime {
            return LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }


    private fun cargarAutosGuardados() {
        try {
            val autosJson = sharedPreferences.getString(AUTOS_KEY, null)
            Log.d(TAG, "Cargando autos guardados. JSON: ${autosJson?.take(100)}...")

            if (!autosJson.isNullOrEmpty()) {
                val listType: Type = object : TypeToken<List<Auto>>() {}.type
                val autosList: List<Auto> = gson.fromJson(autosJson, listType)
                _autos.value = autosList
                Log.d(TAG, "Autos cargados correctamente. Total: ${autosList.size}")
            } else {
                Log.d(TAG, "No hay autos guardados previamente.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar autos: ${e.message}", e)

            _autos.value = emptyList()
        }
    }


    private fun guardarAutosEnPreferences() {
        try {
            val autosJson = gson.toJson(_autos.value)
            Log.d(TAG, "Guardando ${_autos.value.size} autos en SharedPreferences")

            sharedPreferences.edit().apply {
                putString(AUTOS_KEY, autosJson)
                apply()
            }

            Log.d(TAG, "Autos guardados correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar autos: ${e.message}", e)
        }
    }


    private fun generarNuevoId(): Int {
        val maxId = _autos.value.maxOfOrNull { it.auto_id } ?: 0
        return maxId + 1
    }


    private fun autoToMap(auto: Auto): AutoMap {
        return AutoMap().apply {
            put("auto_id", auto.auto_id)
            put("n_serie", auto.n_serie)
            put("sku", auto.sku)
            put("marca_id", auto.marca_id)
            put("modelo", auto.modelo)
            put("anio", auto.anio)
            put("color", auto.color)
            put("precio", auto.precio)
            put("stock", auto.stock)
            put("descripcion", auto.descripcion)
            put("disponibilidad", auto.disponibilidad)
            put("fecha_registro", auto.fecha_registro.toString())
            put("fecha_actualizacion", auto.fecha_actualizacion.toString())
        }
    }


    private fun mapToAuto(map: Map<String, Any>): Auto {
        return try {
            val autoId = (map["auto_id"] as? Number)?.toInt() ?: 0
            val nSerie = map["n_serie"]?.toString() ?: ""
            val sku = map["sku"]?.toString() ?: ""
            val marcaId = (map["marca_id"] as? Number)?.toInt() ?: 1
            val modelo = map["modelo"]?.toString() ?: ""
            val anio = (map["anio"] as? Number)?.toInt() ?: 0
            val color = map["color"]?.toString() ?: ""
            val precio = (map["precio"] as? Number)?.toDouble() ?: 0.0
            val stock = (map["stock"] as? Number)?.toInt() ?: 0
            val descripcion = map["descripcion"]?.toString() ?: ""
            val disponibilidad = map["disponibilidad"] as? Boolean ?: true


            val fechaRegistroStr = map["fecha_registro"]?.toString() ?: LocalDateTime.now().toString()
            val fechaActualizacionStr = map["fecha_actualizacion"]?.toString() ?: LocalDateTime.now().toString()

            val fechaRegistro = try {
                LocalDateTime.parse(fechaRegistroStr)
            } catch (e: Exception) {
                LocalDateTime.now()
            }

            val fechaActualizacion = try {
                LocalDateTime.parse(fechaActualizacionStr)
            } catch (e: Exception) {
                LocalDateTime.now()
            }

            Auto(
                auto_id = autoId,
                n_serie = nSerie,
                sku = sku,
                marca_id = marcaId,
                modelo = modelo,
                anio = anio,
                color = color,
                precio = precio,
                stock = stock,
                descripcion = descripcion,
                disponibilidad = disponibilidad,
                fecha_registro = fechaRegistro,
                fecha_actualizacion = fechaActualizacion
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir Map a Auto: ${e.message}", e)
            Auto()
        }
    }

    // Método para intentar agregar un auto al servidor, con fallback a local
    suspend fun agregarAutoRemoto(auto: Auto): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando guardar auto en el servidor: ${auto.modelo}")


                val autoConId = if (auto.auto_id == 0) {
                    auto.copy(auto_id = generarNuevoId())
                } else {
                    auto
                }


                val autoMap = autoToMap(autoConId)


                val resultado = NetworkUtils.post(ApiClient.AUTO_ENDPOINT, autoMap)

                resultado.fold(
                    onSuccess = { responseBody ->
                        Log.d(TAG, "Éxito al guardar en el servidor, respuesta: $responseBody")


                        agregarAuto(autoConId)


                        if (responseBody.isNotEmpty()) {
                            try {
                                val responseMap = gson.fromJson(responseBody, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                                val autoServidor = mapToAuto(responseMap)
                                Result.success(autoServidor)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar respuesta JSON: ${e.message}")
                                Result.success(autoConId)
                            }
                        } else {
                            Result.success(autoConId)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error del servidor: ${error.message}")


                        agregarAuto(autoConId)
                        Result.failure(Exception("Error en el servidor: ${error.message}"))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error general al agregar auto: ${e.message}", e)


                val autoParaGuardar = if (auto.auto_id == 0) {
                    auto.copy(auto_id = generarNuevoId())
                } else {
                    auto
                }
                agregarAuto(autoParaGuardar)
                Result.failure(e)
            }
        }
    }

    // Método para intentar actualizar un auto en el servidor, con fallback a local
    suspend fun actualizarAutoRemoto(auto: Auto): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando actualizar auto en el servidor: ID=${auto.auto_id}")


                val autoMap = autoToMap(auto)


                val endpoint = "${ApiClient.AUTO_ENDPOINT}/${auto.auto_id}"
                val resultado = NetworkUtils.put(endpoint, autoMap)

                resultado.fold(
                    onSuccess = { responseBody ->
                        Log.d(TAG, "Éxito al actualizar en el servidor, respuesta: $responseBody")


                        actualizarAuto(auto)


                        if (responseBody.isNotEmpty()) {
                            try {
                                val responseMap = gson.fromJson(responseBody, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                                val autoServidor = mapToAuto(responseMap)
                                Result.success(autoServidor)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar respuesta JSON: ${e.message}")
                                Result.success(auto)
                            }
                        } else {
                            Result.success(auto)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error del servidor al actualizar: ${error.message}")


                        actualizarAuto(auto)
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error general al actualizar auto: ${e.message}", e)


                actualizarAuto(auto)
                Result.failure(e)
            }
        }
    }

    // Método para intentar sincronizar los autos del servidor
    suspend fun sincronizarAutosServidor(): Result<List<Auto>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando sincronizar autos desde el servidor")


                val resultado = NetworkUtils.get(ApiClient.AUTO_ENDPOINT)

                resultado.fold(
                    onSuccess = { responseBody ->
                        try {

                            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                            val listaMapas: List<Map<String, Any>> = gson.fromJson(responseBody, listType)
                            val autosServidor = listaMapas.map { mapToAuto(it) }

                            Log.d(TAG, "Autos sincronizados del servidor: ${autosServidor.size}")


                            _autos.value = autosServidor
                            guardarAutosEnPreferences()

                            Result.success(autosServidor)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar la respuesta JSON: ${e.message}")
                            Result.failure(e)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error al obtener autos del servidor: ${error.message}")
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión al sincronizar: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // Método para eliminar un auto en el servidor y localmente
    suspend fun eliminarAutoRemoto(autoId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando eliminar auto del servidor: ID=$autoId")


                val endpoint = "${ApiClient.AUTO_ENDPOINT}/$autoId"
                val resultado = NetworkUtils.delete(endpoint)

                resultado.fold(
                    onSuccess = {
                        Log.d(TAG, "Éxito al eliminar del servidor: ID=$autoId")


                        eliminarAuto(autoId)

                        Result.success(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error del servidor al eliminar: ${error.message}")


                        eliminarAuto(autoId)
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error general al eliminar auto: ${e.message}", e)


                eliminarAuto(autoId)
                Result.failure(e)
            }
        }
    }


    fun existeNumeroSerie(n_serie: String, excludeId: Int = -1): Boolean {
        return _autos.value.any { it.n_serie == n_serie && it.auto_id != excludeId }
    }


    fun existeSku(sku: String, excludeId: Int = -1): Boolean {
        return _autos.value.any { it.sku == sku && it.auto_id != excludeId }
    }


    fun agregarAuto(auto: Auto) {

        if (existeNumeroSerie(auto.n_serie)) {
            Log.e(TAG, "Error: El número de serie ${auto.n_serie} ya existe")
            throw IllegalArgumentException("El número de serie ya existe en otro vehículo")
        }


        if (existeSku(auto.sku)) {
            Log.e(TAG, "Error: El SKU ${auto.sku} ya existe")
            throw IllegalArgumentException("El SKU ya existe en otro vehículo")
        }


        val autoConId = if (auto.auto_id == 0) {
            auto.copy(auto_id = generarNuevoId())
        } else {
            auto
        }

        Log.d(TAG, "Agregando auto: ID=${autoConId.auto_id}, Modelo=${autoConId.modelo}, N° Serie=${autoConId.n_serie}, SKU=${autoConId.sku}")
        _autos.update { currentList ->
            val newList = currentList + autoConId
            newList
        }
        guardarAutosEnPreferences()
    }


    fun actualizarAuto(auto: Auto) {

        if (existeNumeroSerie(auto.n_serie, auto.auto_id)) {
            Log.e(TAG, "Error: El número de serie ${auto.n_serie} ya existe en otro vehículo")
            throw IllegalArgumentException("El número de serie ya existe en otro vehículo")
        }


        if (existeSku(auto.sku, auto.auto_id)) {
            Log.e(TAG, "Error: El SKU ${auto.sku} ya existe en otro vehículo")
            throw IllegalArgumentException("El SKU ya existe en otro vehículo")
        }

        Log.d(TAG, "Actualizando auto con ID: ${auto.auto_id}, Modelo: ${auto.modelo}, N° Serie: ${auto.n_serie}, SKU: ${auto.sku}")
        _autos.update { currentList ->
            currentList.map {
                if (it.auto_id == auto.auto_id) auto else it
            }
        }
        guardarAutosEnPreferences()
    }


    fun eliminarAuto(id: Int) {
        Log.d(TAG, "Eliminando auto con ID: $id")
        _autos.update { currentList ->
            currentList.filter { it.auto_id != id }
        }
        guardarAutosEnPreferences()
    }

    fun obtenerAutoPorId(id: Int): Auto? {
        val auto = _autos.value.find { it.auto_id == id }
        Log.d(TAG, "Buscando auto con ID: $id. Encontrado: ${auto != null}")
        return auto
    }


    fun buscarPorNumeroSerie(n_serie: String): Auto? {
        return _autos.value.find { it.n_serie.equals(n_serie, ignoreCase = true) }
    }


    fun buscarPorSku(sku: String): Auto? {
        return _autos.value.find { it.sku.equals(sku, ignoreCase = true) }
    }


    fun obtenerTodosLosAutos(): List<Auto> {
        return _autos.value
    }

    companion object {
        private const val AUTOS_KEY = "autos_list"

        @Volatile
        private var INSTANCE: AutoRepository? = null


        fun getInstance(context: Context): AutoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AutoRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    // Método para verificar disponibilidad por SKU
    suspend fun verificarDisponibilidadPorSku(sku: String, cantidad: Int): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verificando disponibilidad para SKU: $sku, cantidad: $cantidad")

                // Usar NetworkUtils para la petición
                val endpoint = "${ApiClient.AUTO_ENDPOINT}/verificar-disponibilidad/$sku/$cantidad"
                val resultado = NetworkUtils.get(endpoint)

                resultado.fold(
                    onSuccess = { responseBody ->
                        try {
                            val responseMap = gson.fromJson(responseBody, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
                            Result.success(responseMap)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar respuesta JSON: ${e.message}")
                            Result.failure(e)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error al verificar disponibilidad: ${error.message}")
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error general al verificar disponibilidad: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}