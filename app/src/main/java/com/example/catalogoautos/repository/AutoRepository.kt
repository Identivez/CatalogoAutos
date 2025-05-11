package com.example.catalogoautos.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.network.ApiClient
import com.example.catalogoautos.network.AutoMap
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class AutoRepository(private val context: Context) {

    // Configuración de Gson con adaptadores personalizados para LocalDateTime
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
        .create()

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "autos_preferences", Context.MODE_PRIVATE
    )

    // El StateFlow interno que mantenemos actualizado
    private val _autos = MutableStateFlow<List<Auto>>(emptyList())

    // La versión pública e inmutable que exponemos
    val autos: Flow<List<Auto>> = _autos.asStateFlow()

    init {
        // Cargar los autos guardados cuando se inicializa el repositorio
        cargarAutosGuardados()
    }

    // Adaptador para serializar LocalDateTime a JSON
    private class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }
    }

    // Adaptador para deserializar JSON a LocalDateTime
    private class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LocalDateTime {
            return LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

    // Función para cargar los autos guardados desde SharedPreferences
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
            // Si hay un error al cargar, inicializamos con una lista vacía
            _autos.value = emptyList()
        }
    }

    // Función para guardar los autos en SharedPreferences
    private fun guardarAutosEnPreferences() {
        try {
            val autosJson = gson.toJson(_autos.value)
            Log.d(TAG, "Guardando ${_autos.value.size} autos en SharedPreferences")

            sharedPreferences.edit().apply {
                putString(AUTOS_KEY, autosJson)
                apply() // Aplicar los cambios de forma asíncrona
            }

            Log.d(TAG, "Autos guardados correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar autos: ${e.message}", e)
        }
    }

    // Función para generar un nuevo ID para un auto
    private fun generarNuevoId(): Int {
        val maxId = _autos.value.maxOfOrNull { it.auto_id } ?: 0
        return maxId + 1
    }

    // Función para convertir un Auto a HashMap para la API
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

    // Función para convertir un Map de la API a Auto
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

            // Parsear fechas
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
            Auto() // Retornar un auto vacío en caso de error
        }
    }

    // Función para probar diferentes URLs
    suspend fun probarConexiones() {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val urls = listOf(
                "http://192.168.1.14:8080",
                "http://192.168.1.14:8080/api",
                "http://192.168.1.14:8080/api/auto",
                "http://192.168.1.14:8080/auto"
            )

            for (url in urls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    Log.d(TAG, "URL: $url - Código: ${response.code}")

                    if (response.isSuccessful) {
                        Log.d(TAG, "URL EXITOSA: $url")
                        // Guardar preferencia de URL si es necesario
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al probar URL $url: ${e.message}")
                }
            }
        }
    }

    // Método alternativo que usa directamente OkHttp en caso de problemas con Retrofit
    suspend fun agregarAutoDirecto(auto: Auto): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando guardar auto directamente con OkHttp: ${auto.modelo}")

                // Asignar un nuevo ID si es 0
                val autoConId = if (auto.auto_id == 0) {
                    auto.copy(auto_id = generarNuevoId())
                } else {
                    auto
                }

                // Cliente OkHttp
                val client = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                // Convertir a JSON
                val json = gson.toJson(autoToMap(autoConId))

                // URL explícita y completa
                val url = "http://192.168.1.14:8080/api/auto"

                // Crear el cuerpo de la petición
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

                // Crear la petición
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                // Ejecutar la petición
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d(TAG, "Éxito al guardar en el servidor usando OkHttp directo")

                    // Guardar localmente también
                    agregarAuto(autoConId)

                    Result.success(autoConId)
                } else {
                    Log.e(TAG, "Error del servidor usando OkHttp directo: ${response.code} - ${response.message}")

                    // Si el servidor falla, guardar localmente de todos modos
                    agregarAuto(autoConId)
                    Result.failure(Exception("Error en el servidor: ${response.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión usando OkHttp directo: ${e.message}", e)

                // Manejar el error creando autoParaGuardar dentro de este ámbito
                val autoParaGuardar = if (auto.auto_id == 0) {
                    auto.copy(auto_id = generarNuevoId())
                } else {
                    auto
                }

                // Si hay excepción (error de conexión), guardar localmente
                agregarAuto(autoParaGuardar)
                Result.failure(e)
            }
        }
    }

    // Método para intentar agregar un auto al servidor, con fallback a local
    suspend fun agregarAutoRemoto(auto: Auto): Result<Auto> {
        // Probar URLs primero
        probarConexiones()

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando guardar auto en el servidor: ${auto.modelo}")

                // Imprimir la URL que usará Retrofit
                val urlCompleta = ApiClient.getBaseUrl() + "auto"
                Log.d(TAG, "URL completa para la petición: $urlCompleta")

                // Asignar un nuevo ID si es 0
                val autoConId = if (auto.auto_id == 0) {
                    auto.copy(auto_id = generarNuevoId())
                } else {
                    auto
                }

                // Convertir el auto a HashMap para la API
                val autoMap = autoToMap(autoConId)

                // Intentar guardar en el servidor
                val response = ApiClient.autoApi.agregarAuto(autoMap)

                if (response.isSuccessful) {
                    Log.d(TAG, "Éxito al guardar en el servidor. Código: ${response.code()}")

                    // Convertir la respuesta a Auto
                    val autoServidor = response.body()?.let { mapToAuto(it) } ?: autoConId

                    // Agregar a la lista local
                    _autos.update { currentList ->
                        val newList = currentList + autoServidor
                        newList
                    }
                    guardarAutosEnPreferences()

                    Result.success(autoServidor)
                } else {
                    Log.e(TAG, "Error del servidor: ${response.code()} - ${response.message()}")

                    // Si el servidor falla con 404, intentar con el método directo
                    if (response.code() == 404) {
                        Log.d(TAG, "Intentando método alternativo directo con OkHttp")
                        return@withContext agregarAutoDirecto(auto)
                    }

                    // Si el servidor falla, guardar localmente de todos modos
                    agregarAuto(autoConId)
                    Result.failure(Exception("Error en el servidor: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión: ${e.message}", e)

                // Si hay un error de Retrofit, intentar con el método directo
                Log.d(TAG, "Intentando método alternativo directo con OkHttp debido a excepción")
                try {
                    return@withContext agregarAutoDirecto(auto)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error también con el método directo: ${e2.message}")

                    // Si ambos métodos fallan, guardar localmente como último recurso
                    val autoParaGuardar = if (auto.auto_id == 0) {
                        auto.copy(auto_id = generarNuevoId())
                    } else {
                        auto
                    }

                    agregarAuto(autoParaGuardar)
                    Result.failure(e2)
                }
            }
        }
    }

    // Método para intentar actualizar un auto en el servidor, con fallback a local
    suspend fun actualizarAutoRemoto(auto: Auto): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Intentando actualizar auto en el servidor: ID=${auto.auto_id}")

                // Imprimir la URL que usará Retrofit
                val urlCompleta = ApiClient.getBaseUrl() + "auto/" + auto.auto_id
                Log.d(TAG, "URL completa para la petición: $urlCompleta")

                // Convertir el auto a HashMap para la API
                val autoMap = autoToMap(auto)

                // Intentar actualizar en el servidor
                val response = ApiClient.autoApi.actualizarAuto(auto.auto_id, autoMap)

                if (response.isSuccessful) {
                    Log.d(TAG, "Éxito al actualizar en el servidor. Código: ${response.code()}")

                    // Convertir la respuesta a Auto
                    val autoServidor = response.body()?.let { mapToAuto(it) } ?: auto

                    // Actualizar la versión local
                    actualizarAuto(autoServidor)

                    Result.success(autoServidor)
                } else {
                    Log.e(TAG, "Error del servidor al actualizar: ${response.code()} - ${response.message()}")

                    // Si el servidor falla, actualizar localmente de todos modos
                    actualizarAuto(auto)
                    Result.failure(Exception("Error en el servidor: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión al actualizar: ${e.message}", e)

                // Si hay excepción, actualizar localmente
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

                // Obtener los autos del servidor
                val response = ApiClient.autoApi.obtenerTodos()

                if (response.isSuccessful) {
                    // Convertir la respuesta a Lista de Autos
                    val autosServidor = response.body()?.map { mapToAuto(it) } ?: emptyList()

                    Log.d(TAG, "Autos obtenidos del servidor: ${autosServidor.size}")

                    // Actualizar la caché local
                    _autos.value = autosServidor
                    guardarAutosEnPreferences()

                    Result.success(autosServidor)
                } else {
                    Log.e(TAG, "Error al obtener autos del servidor: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Error al obtener autos: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión al sincronizar: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Función para verificar si un número de serie ya existe
    fun existeNumeroSerie(n_serie: String, excludeId: Int = -1): Boolean {
        return _autos.value.any { it.n_serie == n_serie && it.auto_id != excludeId }
    }

    // Función para verificar si un SKU ya existe
    fun existeSku(sku: String, excludeId: Int = -1): Boolean {
        return _autos.value.any { it.sku == sku && it.auto_id != excludeId }
    }

    // Función para agregar un nuevo auto con validación de campos únicos
    fun agregarAuto(auto: Auto) {
        // Validar que el número de serie sea único
        if (existeNumeroSerie(auto.n_serie)) {
            Log.e(TAG, "Error: El número de serie ${auto.n_serie} ya existe")
            throw IllegalArgumentException("El número de serie ya existe en otro vehículo")
        }

        // Validar que el SKU sea único
        if (existeSku(auto.sku)) {
            Log.e(TAG, "Error: El SKU ${auto.sku} ya existe")
            throw IllegalArgumentException("El SKU ya existe en otro vehículo")
        }

        // Asignar un nuevo ID si es 0 (valor por defecto)
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

    // Función para actualizar un auto existente con validación de campos únicos
    fun actualizarAuto(auto: Auto) {
        // Validar que el número de serie sea único (excluyendo el auto actual)
        if (existeNumeroSerie(auto.n_serie, auto.auto_id)) {
            Log.e(TAG, "Error: El número de serie ${auto.n_serie} ya existe en otro vehículo")
            throw IllegalArgumentException("El número de serie ya existe en otro vehículo")
        }

        // Validar que el SKU sea único (excluyendo el auto actual)
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

    // Función para eliminar un auto
    fun eliminarAuto(id: Int) {
        Log.d(TAG, "Eliminando auto con ID: $id")
        _autos.update { currentList ->
            currentList.filter { it.auto_id != id }
        }
        guardarAutosEnPreferences()
    }

    // Función para obtener un auto por su ID
    fun obtenerAutoPorId(id: Int): Auto? {
        val auto = _autos.value.find { it.auto_id == id }
        Log.d(TAG, "Buscando auto con ID: $id. Encontrado: ${auto != null}")
        return auto
    }

    // Función para buscar autos por número de serie
    fun buscarPorNumeroSerie(n_serie: String): Auto? {
        return _autos.value.find { it.n_serie.equals(n_serie, ignoreCase = true) }
    }

    // Función para buscar autos por SKU
    fun buscarPorSku(sku: String): Auto? {
        return _autos.value.find { it.sku.equals(sku, ignoreCase = true) }
    }

    // Obtener todos los autos como una lista (uso síncrono)
    fun obtenerTodosLosAutos(): List<Auto> {
        return _autos.value
    }

    companion object {
        private const val TAG = "AutoRepository"
        private const val AUTOS_KEY = "autos_list"

        @Volatile
        private var INSTANCE: AutoRepository? = null

        // Actualizado para requerir un contexto
        fun getInstance(context: Context): AutoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AutoRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}