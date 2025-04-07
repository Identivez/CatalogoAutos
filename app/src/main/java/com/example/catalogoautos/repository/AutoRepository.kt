package com.example.catalogoautos.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.catalogoautos.model.Auto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.reflect.Type

class AutoRepository private constructor(private val context: Context) {
    private val gson = Gson()
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

    // Función para agregar un nuevo auto
    fun agregarAuto(auto: Auto) {
        // Asignar un nuevo ID si es 0 (valor por defecto)
        val autoConId = if (auto.auto_id == 0) {
            auto.copy(auto_id = generarNuevoId())
        } else {
            auto
        }

        Log.d(TAG, "Agregando auto: ID=${autoConId.auto_id}, Modelo=${autoConId.modelo}")
        _autos.update { currentList ->
            val newList = currentList + autoConId
            newList
        }
        guardarAutosEnPreferences()
    }

    // Función para actualizar un auto existente
    fun actualizarAuto(auto: Auto) {
        Log.d(TAG, "Actualizando auto con ID: ${auto.auto_id}")
        _autos.update { currentList ->
            currentList.map {
                if (it.auto_id == auto.auto_id) auto else it
            }
        }
        guardarAutosEnPreferences()
    }

    // Función para obtener un auto por su ID
    fun obtenerAutoPorId(id: Int): Auto? {
        val auto = _autos.value.find { it.auto_id == id }
        Log.d(TAG, "Buscando auto con ID: $id. Encontrado: ${auto != null}")
        return auto
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