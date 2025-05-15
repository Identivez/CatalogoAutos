package com.example.catalogoautos.repository

import Venta
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.catalogoautos.model.VentaRequest
import com.example.catalogoautos.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext

class VentasRepository(private val context: Context) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val TAG = "VentasRepository"

    // Constantes para los estados de venta
    companion object {
        // Estados válidos según la restricción check de la base de datos
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_ENTREGADA = "ENTREGADA"
        const val ESTADO_CANCELADA = "CANCELADA"

        // Lista de estados válidos para referencia
        val ESTADOS_VALIDOS = listOf(ESTADO_PENDIENTE, ESTADO_ENTREGADA, ESTADO_CANCELADA)

        @Volatile
        private var INSTANCE: VentasRepository? = null

        fun getInstance(context: Context): VentasRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = VentasRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // Método para convertir Map a Venta
    private fun mapToVenta(map: Map<String, Any>): Venta {
        try {
            val ventaId = (map["ventaId"] as? Number)?.toInt() ?: 0
            val nSerie = map["nSerie"]?.toString() ?: ""
            val cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1
            val precioStr = map["precio"]?.toString() ?: "0"
            val estatus = map["estatus"]?.toString() ?: ESTADO_PENDIENTE

            // Parsear fecha
            val fechaStr = map["fechaVenta"]?.toString() ?: LocalDateTime.now().toString()
            val fecha = try {
                if (fechaStr.contains("T")) {
                    LocalDateTime.parse(fechaStr)
                } else {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    LocalDateTime.parse(fechaStr, formatter)
                }
            } catch (e: Exception) {
                LocalDateTime.now()
            }

            return Venta(
                ventaId = ventaId,
                nSerie = nSerie,
                cantidad = cantidad,
                precio = BigDecimal(precioStr),
                estatus = estatus,
                fechaVenta = fecha
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir Map a Venta: ${e.message}")
            return Venta(
                nSerie = "",
                precio = BigDecimal.ZERO
            )
        }
    }

    // Obtener todas las ventas
    fun getAllVentas(callback: (List<Venta>?, String?) -> Unit) {
        launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.obtenerTodasLasVentas()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val ventas = responseBody?.map { mapToVenta(it) } ?: emptyList()
                    callback(ventas, null)
                } else {
                    Log.e(TAG, "Error del servidor: ${response.code()}")
                    callback(null, "Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener ventas", e)
                callback(null, "Error de conexión: ${e.message}")
            }
        }
    }

    // Obtener venta por ID
    fun getVentaById(id: Int, callback: (Venta?, String?) -> Unit) {
        launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.obtenerVentaPorId(id)
                }

                if (response.isSuccessful) {
                    val venta = response.body()?.let { mapToVenta(it) }
                    callback(venta, null)
                } else {
                    Log.e(TAG, "Error del servidor: ${response.code()}")
                    callback(null, "Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener venta por ID", e)
                callback(null, "Error de conexión: ${e.message}")
            }
        }
    }

    // Obtener ventas por estatus
    fun getVentasByEstatus(estatus: String, callback: (List<Venta>?, String?) -> Unit) {
        launch {
            try {
                // Validar que el estatus proporcionado sea válido
                val estatusValido = if (estatus in ESTADOS_VALIDOS) estatus else ESTADO_PENDIENTE

                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.obtenerVentasPorEstatus(estatusValido)
                }

                if (response.isSuccessful) {
                    val ventas = response.body()?.map { mapToVenta(it) } ?: emptyList()
                    callback(ventas, null)
                } else {
                    Log.e(TAG, "Error del servidor: ${response.code()}")
                    callback(null, "Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener ventas por estatus", e)
                callback(null, "Error de conexión: ${e.message}")
            }
        }
    }

    // Registrar venta
    fun registrarVenta(nSerie: String, cantidad: Int, precio: BigDecimal, callback: (Venta?, String?) -> Unit) {
        launch {
            try {
                // Crear una instancia de VentaRequest con el estatus PENDIENTE
                val ventaRequest = VentaRequest(
                    nSerie = nSerie,
                    cantidad = cantidad,
                    precio = precio,
                    estatus = ESTADO_PENDIENTE // Usar el valor válido según la restricción check
                )

                Log.d(TAG, "Enviando registro de venta con estado: ${ventaRequest.estatus}")

                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.registrarVenta(ventaRequest)
                }

                if (response.isSuccessful) {
                    val venta = response.body()?.let { mapToVenta(it) }
                    callback(venta, null)
                } else {
                    var errorMsg = "Error del servidor: ${response.code()}"
                    try {
                        // Intentar extraer el mensaje de error de forma más limpia
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            // Intentar extraer solo el mensaje relevante del HTML/texto
                            val cleanError = extractCleanErrorMessage(errorBody)
                            if (cleanError.isNotEmpty()) {
                                errorMsg = cleanError
                            } else {
                                errorMsg = "Error al registrar venta. Por favor, inténtalo de nuevo."
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al leer mensaje de error", e)
                    }

                    Log.e(TAG, errorMsg)
                    callback(null, errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar venta", e)
                callback(null, "Error de conexión: ${e.message}")
            }
        }
    }

    // Actualizar estatus
    fun actualizarEstatus(id: Int, estatus: String, callback: (Venta?, String?) -> Unit) {
        launch {
            try {
                // Validar que el estatus proporcionado sea válido
                val estatusValido = if (estatus in ESTADOS_VALIDOS) estatus else ESTADO_PENDIENTE

                val estatusMap = mapOf("estatus" to estatusValido)

                Log.d(TAG, "Actualizando estatus a: $estatusValido")

                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.actualizarEstatus(id, estatusMap)
                }

                if (response.isSuccessful) {
                    val venta = response.body()?.let { mapToVenta(it) }
                    callback(venta, null)
                } else {
                    var errorMsg = "Error del servidor: ${response.code()}"
                    try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            val cleanError = extractCleanErrorMessage(errorBody)
                            if (cleanError.isNotEmpty()) {
                                errorMsg = cleanError
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al leer mensaje de error", e)
                    }

                    Log.e(TAG, errorMsg)
                    callback(null, errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar estatus", e)
                callback(null, "Error de conexión: ${e.message}")
            }
        }
    }

    // Cancelar venta
    fun cancelarVenta(id: Int, callback: (Boolean, String?) -> Unit) {
        launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.cancelarVenta(id)
                }

                if (response.isSuccessful) {
                    callback(true, "Venta cancelada correctamente")
                } else {
                    var errorMsg = "Error del servidor: ${response.code()}"
                    try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            val cleanError = extractCleanErrorMessage(errorBody)
                            if (cleanError.isNotEmpty()) {
                                errorMsg = cleanError
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al leer mensaje de error", e)
                    }

                    Log.e(TAG, errorMsg)
                    callback(false, errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cancelar venta", e)
                callback(false, "Error de conexión: ${e.message}")
            }
        }
    }

    // Contar ventas por estatus
    fun contarVentas(callback: (Map<String, Int>?, String?) -> Unit) {
        launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.contarVentas()
                }

                if (response.isSuccessful) {
                    val counts = response.body()
                    val countsMap = mutableMapOf<String, Int>()

                    // Función auxiliar para extraer números de forma segura
                    fun safeExtractInt(value: Any?): Int {
                        return when (value) {
                            null -> 0
                            is Int -> value
                            is Number -> value.toInt()
                            is String -> value.toIntOrNull() ?: 0
                            else -> 0
                        }
                    }

                    // Extraer valores con la función auxiliar
                    if (counts != null) {
                        countsMap["total"] = safeExtractInt(counts["total"])
                        countsMap["pendientes"] = safeExtractInt(counts["pendientes"])
                        countsMap["entregadas"] = safeExtractInt(counts["entregadas"])
                        countsMap["canceladas"] = safeExtractInt(counts["canceladas"])
                        // Usamos "completadas" para mantener compatibilidad con el código existente,
                        // pero en realidad podría no existir este campo si la DB no lo permite
                        countsMap["completadas"] = safeExtractInt(counts["completadas"])
                    } else {
                        // Valores por defecto
                        countsMap["total"] = 0
                        countsMap["pendientes"] = 0
                        countsMap["entregadas"] = 0
                        countsMap["canceladas"] = 0
                        countsMap["completadas"] = 0
                    }

                    callback(countsMap, null)
                } else {
                    Log.e(TAG, "Error del servidor: ${response.code()}")
                    callback(null, "Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al contar ventas", e)
                callback(null, "Error de conexión: ${e.message}")
            }
        }
    }

    // Método para extraer mensajes de error limpios de respuestas HTML
    private fun extractCleanErrorMessage(htmlResponse: String): String {
        // Intentar encontrar el mensaje de error específico en la respuesta HTML
        val patterns = listOf(
            // Patrones comunes para extraer mensajes de error
            "violación de la restricción «ventas_estatus_check»",
            "Error: ([^<]+)",
            "Detail: ([^<]+)",
            "message[^>]+>([^<]+)",
            "description[^>]+>([^<]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(htmlResponse)
            if (matchResult != null) {
                val errorMessage = matchResult.groups[1]?.value ?: matchResult.value
                return errorMessage.trim()
            }
        }

        // Si no podemos extraer un mensaje específico, devolver un mensaje genérico
        if (htmlResponse.contains("ventas_estatus_check")) {
            return "El estado proporcionado no es válido. Por favor, use uno de los siguientes: ${ESTADOS_VALIDOS.joinToString()}"
        }

        return "Error al procesar la solicitud. Por favor, inténtalo de nuevo."
    }
}