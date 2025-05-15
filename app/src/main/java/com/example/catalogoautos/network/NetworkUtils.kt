package com.example.catalogoautos.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * Realiza una petición GET
     */
    suspend fun get(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = ApiClient.getFullUrl(endpoint)
            Log.d(TAG, "GET request to: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            // Realizar la petición en un hilo de fondo
            val response = ApiClient.httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "GET successful, response: ${body.take(100)}...")
                Result.success(body)
            } else {
                Log.e(TAG, "Error in GET to $url: ${response.code} - ${response.message}")
                Result.failure(IOException("Error ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in GET to $endpoint: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Realiza una petición POST con body JSON
     */
    suspend fun post(endpoint: String, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = ApiClient.getFullUrl(endpoint)
            Log.d(TAG, "POST request to: $url")
            Log.d(TAG, "POST body: $jsonString")

            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // Realizar la petición en un hilo de fondo
            val response = ApiClient.httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "POST successful, response: ${body.take(100)}...")
                Result.success(body)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Error in POST to $url: ${response.code} - ${response.message}")
                Log.e(TAG, "Error body: $errorBody")
                Result.failure(IOException("Error ${response.code}: ${response.message}\nError body: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in POST to $endpoint: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Realiza una petición POST con un Map como body
     */
    suspend fun post(endpoint: String, bodyMap: Map<String, Any>): Result<String> {
        val jsonBody = JSONObject(bodyMap).toString()
        return post(endpoint, jsonBody)
    }

    /**
     * Realiza una petición PUT con body JSON
     */
    suspend fun put(endpoint: String, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = ApiClient.getFullUrl(endpoint)
            Log.d(TAG, "PUT request to: $url")
            Log.d(TAG, "PUT body: $jsonString")

            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .build()

            // Realizar la petición en un hilo de fondo
            val response = ApiClient.httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "PUT successful, response: ${body.take(100)}...")
                Result.success(body)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Error in PUT to $url: ${response.code} - ${response.message}")
                Log.e(TAG, "Error body: $errorBody")
                Result.failure(IOException("Error ${response.code}: ${response.message}\nError body: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in PUT to $endpoint: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Realiza una petición PUT con un Map como body
     */
    suspend fun put(endpoint: String, bodyMap: Map<String, Any>): Result<String> {
        val jsonBody = JSONObject(bodyMap).toString()
        return put(endpoint, jsonBody)
    }

    /**
     * Realiza una petición DELETE
     */
    suspend fun delete(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = ApiClient.getFullUrl(endpoint)
            Log.d(TAG, "DELETE request to: $url")

            val request = Request.Builder()
                .url(url)
                .delete()
                .build()

            // Realizar la petición en un hilo de fondo
            val response = ApiClient.httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "DELETE successful, response: ${body.take(100)}...")
                Result.success(body)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Error in DELETE to $url: ${response.code} - ${response.message}")
                Log.e(TAG, "Error body: $errorBody")
                Result.failure(IOException("Error ${response.code}: ${response.message}\nError body: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in DELETE to $endpoint: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Verifica la conectividad con el servidor
     */
    suspend fun verificarConectividad(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = ApiClient.getBaseUrl()
            val request = Request.Builder()
                .url(url)
                .head()  // HEAD es más ligero que GET
                .build()

            val response = ApiClient.httpClient.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar conectividad: ${e.message}")
            return@withContext false
        }
    }
}