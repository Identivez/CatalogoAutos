// VentasService.kt
package com.example.catalogoautos.api

import android.os.Handler
import android.os.Looper
import com.example.catalogoautos.model.Venta
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class VentasService {
    private val BASE_URL = "http://10.250.3.8:8080/ae_byd/api/ventas/"
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    // Método auxiliar para ejecutar código en segundo plano y retornar en el hilo principal
    private fun <T> executeAsync(
        backgroundTask: () -> T,
        callback: (result: T?, error: String?) -> Unit
    ) {
        executor.execute {
            try {
                val result = backgroundTask()
                handler.post { callback(result, null) }
            } catch (e: Exception) {
                handler.post { callback(null, e.message) }
            }
        }
    }

    // Convertir JSONObject a objeto Venta
    private fun parseVenta(jsonObject: JSONObject): Venta {
        val fechaStr = jsonObject.optString("fechaVenta")
        val fecha = try {
            // Convertir la cadena de fecha a LocalDateTime
            if (fechaStr.isNotEmpty()) {
                if (fechaStr.contains("T")) {
                    // Formato ISO
                    java.time.LocalDateTime.parse(fechaStr)
                } else {
                    // Formato de timestamp SQL
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    java.time.LocalDateTime.parse(fechaStr, formatter)
                }
            } else {
                java.time.LocalDateTime.now()
            }
        } catch (e: Exception) {
            java.time.LocalDateTime.now()
        }

        return Venta(
            ventaId = jsonObject.optInt("ventaId"),
            nSerie = jsonObject.optString("nSerie"),
            cantidad = jsonObject.optInt("cantidad"),
            precio = BigDecimal(jsonObject.optString("precio", "0")),
            estatus = jsonObject.optString("estatus"),
            fechaVenta = fecha
        )
    }

    // Obtener todas las ventas
    fun obtenerTodasLasVentas(callback: (List<Venta>?, String?) -> Unit) {
        executeAsync({
            val url = URL(BASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonArray = JSONArray(response)
                val ventas = mutableListOf<Venta>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    ventas.add(parseVenta(jsonObject))
                }

                ventas
            } else {
                throw Exception("Error: ${connection.responseMessage}")
            }
        }, callback)
    }

    // Obtener venta por ID
    fun obtenerVentaPorId(id: Int, callback: (Venta?, String?) -> Unit) {
        executeAsync({
            val url = URL("$BASE_URL$id")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                parseVenta(JSONObject(response))
            } else {
                throw Exception("Error: ${connection.responseMessage}")
            }
        }, callback)
    }

    // Registrar una nueva venta
    fun registrarVenta(nSerie: String, cantidad: Int, precio: BigDecimal, callback: (Venta?, String?) -> Unit) {
        executeAsync({
            val url = URL(BASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonObject = JSONObject().apply {
                put("nSerie", nSerie)
                put("cantidad", cantidad)
                put("precio", precio.toString())
            }

            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(jsonObject.toString())
            outputStream.flush()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                parseVenta(JSONObject(response))
            } else {
                throw Exception("Error: ${connection.responseMessage}")
            }
        }, callback)
    }

    // Actualizar el estado de una venta
    fun actualizarEstatus(id: Int, estatus: String, callback: (Venta?, String?) -> Unit) {
        executeAsync({
            val url = URL("$BASE_URL$id/estatus")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonObject = JSONObject().apply {
                put("estatus", estatus)
            }

            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(jsonObject.toString())
            outputStream.flush()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                parseVenta(JSONObject(response))
            } else {
                throw Exception("Error: ${connection.responseMessage}")
            }
        }, callback)
    }
    // Cancelar una venta
    fun cancelarVenta(id: Int, callback: (Boolean, String?) -> Unit) {
        executor.execute {
            try {
                val url = URL("$BASE_URL$id")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"

                val responseCode = connection.responseCode
                val success = responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT

                // Usar el handler para enviar el resultado al hilo principal
                handler.post { callback(success, null) }
            } catch (e: Exception) {
                // En caso de error, devolver false y el mensaje de error
                handler.post { callback(false, e.message) }
            }
        }
    }
}