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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.CoroutineContext


class VentasRepository(private val context: Context) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val TAG = "VentasRepository"
    private val BASE_URL = "http://10.250.3.8:8080/AE_BYD/api"
    private val client = OkHttpClient()


    companion object {

        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_ENTREGADA = "ENTREGADA"
        const val ESTADO_CANCELADA = "CANCELADA"


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


    fun registrarVenta(nSerie: String, cantidad: Int, precio: BigDecimal, callback: (Venta?, String?) -> Unit) {
        launch {
            try {

                val ventaRequest = VentaRequest(
                    nSerie = nSerie,
                    cantidad = cantidad,
                    precio = precio,
                    estatus = ESTADO_PENDIENTE
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

                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {

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


    fun actualizarEstatus(id: Int, estatus: String, callback: (Venta?, String?) -> Unit) {
        launch {
            try {

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

    fun contarVentas(callback: (Map<String, Int>?, String?) -> Unit) {
        launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.ventasApi.contarVentas()
                }

                if (response.isSuccessful) {
                    val counts = response.body()
                    val countsMap = mutableMapOf<String, Int>()


                    fun safeExtractInt(value: Any?): Int {
                        return when (value) {
                            null -> 0
                            is Int -> value
                            is Number -> value.toInt()
                            is String -> value.toIntOrNull() ?: 0
                            else -> 0
                        }
                    }


                    if (counts != null) {
                        countsMap["total"] = safeExtractInt(counts["total"])
                        countsMap["pendientes"] = safeExtractInt(counts["pendientes"])
                        countsMap["entregadas"] = safeExtractInt(counts["entregadas"])
                        countsMap["canceladas"] = safeExtractInt(counts["canceladas"])

                        countsMap["completadas"] = safeExtractInt(counts["completadas"])
                    } else {

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


        if (htmlResponse.contains("ventas_estatus_check")) {
            return "El estado proporcionado no es válido. Por favor, use uno de los siguientes: ${ESTADOS_VALIDOS.joinToString()}"
        }

        return "Error al procesar la solicitud. Por favor, inténtalo de nuevo."
    }


    interface VentasCallback {
        fun onVentasRecibidas(ventas: List<Venta>)
        fun onError(mensaje: String)
    }


    fun getVentasPorFiltro(
        fechaInicio: LocalDateTime,
        fechaFin: LocalDateTime,
        estatus: String?,
        callback: VentasCallback
    ) {
        launch {
            try {
                val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val fechaInicioStr = fechaInicio.format(dateFormat)
                val fechaFinStr = fechaFin.format(dateFormat)

                var url = "$BASE_URL/ventas/filtro?fechaInicio=$fechaInicioStr&fechaFin=$fechaFinStr"

                if (!estatus.isNullOrEmpty() && estatus.lowercase() != "todos") {
                    url += "&estatus=$estatus"
                }

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val ventas = parsearRespuesta(responseBody)
                    callback.onVentasRecibidas(ventas)
                } else {
                    callback.onError("Error al obtener ventas: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en la solicitud", e)
                callback.onError("Error en la comunicación: ${e.message}")
            }
        }
    }


    private fun parsearRespuesta(jsonResponse: String?): List<Venta> {
        if (jsonResponse.isNullOrEmpty()) return emptyList()

        val ventas = mutableListOf<Venta>()
        try {
            val jsonArray = JSONArray(jsonResponse)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val ventaId = jsonObject.getInt("ventaId")
                val nSerie = jsonObject.getString("nSerie")
                val cantidad = jsonObject.getInt("cantidad")
                val precio = BigDecimal(jsonObject.getString("precio"))
                val estatus = jsonObject.getString("estatus")


                val fechaStr = jsonObject.getString("fechaVenta")
                val fecha = try {

                    val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                    val date = sdf.parse(fechaStr)


                    val instant = date.toInstant()
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                } catch (e: Exception) {
                    Log.e(TAG, "Error con SimpleDateFormat, intentando formatos alternativos para: $fechaStr", e)


                    try {
                        if (fechaStr.contains("T")) {

                            LocalDateTime.parse(fechaStr, DateTimeFormatter.ISO_DATE_TIME)
                        } else if (fechaStr.contains("-") && !fechaStr.contains(":")) {

                            LocalDate.parse(fechaStr).atStartOfDay()
                        } else {

                            Log.e(TAG, "No se pudo parsear la fecha: $fechaStr, usando fecha actual", e)
                            LocalDateTime.now()
                        }
                    } catch (innerE: Exception) {
                        Log.e(TAG, "Fallo definitivo al parsear fecha: $fechaStr", innerE)
                        LocalDateTime.now()
                    }
                }

                val venta = Venta(ventaId, nSerie, cantidad, precio, estatus, fecha)
                ventas.add(venta)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear respuesta JSON", e)
        }
        return ventas
    }
}