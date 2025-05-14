// VentasRepository.kt
package com.example.catalogoautos.repository

import android.content.Context
import android.util.Log
import com.example.catalogoautos.model.Venta
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class VentasRepository(private val context: Context) {

    private val TAG = "VentasRepository"
    private val BASE_URL = "http://10.250.3.8:8080/ae_byd/api/ventas/"

    // Configurar cliente HTTP con logging
    private val client: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getAllVentas(callback: (List<Venta>?, String?) -> Unit) {
        Thread {
            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val ventas = parseVentasResponse(responseBody)

                    // Retornar en el hilo principal
                    android.os.Handler(context.mainLooper).post {
                        callback(ventas, null)
                    }
                } else {
                    // Error de servidor
                    android.os.Handler(context.mainLooper).post {
                        callback(null, "Error del servidor: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener ventas", e)
                android.os.Handler(context.mainLooper).post {
                    callback(null, "Error de conexión: ${e.message}")
                }
            }
        }.start()
    }

    fun getVentaById(id: Int, callback: (Venta?, String?) -> Unit) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL$id")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val venta = parseVentaResponse(responseBody)

                    android.os.Handler(context.mainLooper).post {
                        callback(venta, null)
                    }
                } else {
                    android.os.Handler(context.mainLooper).post {
                        callback(null, "Error del servidor: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener venta por ID", e)
                android.os.Handler(context.mainLooper).post {
                    callback(null, "Error de conexión: ${e.message}")
                }
            }
        }.start()
    }

    fun getVentasByEstatus(estatus: String, callback: (List<Venta>?, String?) -> Unit) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("${BASE_URL}estatus/$estatus")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val ventas = parseVentasResponse(responseBody)

                    android.os.Handler(context.mainLooper).post {
                        callback(ventas, null)
                    }
                } else {
                    android.os.Handler(context.mainLooper).post {
                        callback(null, "Error del servidor: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener ventas por estatus", e)
                android.os.Handler(context.mainLooper).post {
                    callback(null, "Error de conexión: ${e.message}")
                }
            }
        }.start()
    }

    fun registrarVenta(nSerie: String, cantidad: Int, precio: BigDecimal, callback: (Venta?, String?) -> Unit) {
        Thread {
            try {
                val json = JSONObject().apply {
                    put("nSerie", nSerie)
                    put("cantidad", cantidad)
                    put("precio", precio.toString())
                }

                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(BASE_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val venta = parseVentaResponse(responseBody)

                    android.os.Handler(context.mainLooper).post {
                        callback(venta, null)
                    }
                } else {
                    android.os.Handler(context.mainLooper).post {
                        callback(null, "Error del servidor: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar venta", e)
                android.os.Handler(context.mainLooper).post {
                    callback(null, "Error de conexión: ${e.message}")
                }
            }
        }.start()
    }

    fun actualizarEstatus(id: Int, estatus: String, callback: (Venta?, String?) -> Unit) {
        Thread {
            try {
                val json = JSONObject().apply {
                    put("estatus", estatus)
                }

                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL$id/estatus")
                    .put(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val venta = parseVentaResponse(responseBody)

                    android.os.Handler(context.mainLooper).post {
                        callback(venta, null)
                    }
                } else {
                    android.os.Handler(context.mainLooper).post {
                        callback(null, "Error del servidor: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar estatus", e)
                android.os.Handler(context.mainLooper).post {
                    callback(null, "Error de conexión: ${e.message}")
                }
            }
        }.start()
    }

    fun cancelarVenta(id: Int, callback: (Boolean, String?) -> Unit) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL$id")
                    .delete()
                    .build()

                val response = client.newCall(request).execute()

                android.os.Handler(context.mainLooper).post {
                    if (response.isSuccessful) {
                        callback(true, null)
                    } else {
                        callback(false, "Error del servidor: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cancelar venta", e)
                android.os.Handler(context.mainLooper).post {
                    callback(false, "Error de conexión: ${e.message}")
                }
            }
        }.start()
    }

    // Métodos auxiliares para parsear las respuestas
    private fun parseVentasResponse(jsonString: String?): List<Venta> {
        if (jsonString.isNullOrEmpty()) {
            return emptyList()
        }

        val ventas = mutableListOf<Venta>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                ventas.add(parseVentaJson(jsonObject))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear ventas", e)
        }

        return ventas
    }

    private fun parseVentaResponse(jsonString: String?): Venta? {
        if (jsonString.isNullOrEmpty()) {
            return null
        }

        return try {
            val jsonObject = JSONObject(jsonString)
            parseVentaJson(jsonObject)
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear venta", e)
            null
        }
    }

    private fun parseVentaJson(jsonObject: JSONObject): Venta {
        return Venta.fromJson(jsonObject.toString())
    }

    companion object {
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
}