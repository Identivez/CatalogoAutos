package com.example.catalogoautos.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.catalogoautos.model.Auto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AutoRepository private constructor(private val context: Context) {
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
        override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }
    }

    // Adaptador para deserializar JSON a LocalDateTime
    private class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
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