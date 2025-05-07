package com.example.catalogoautos.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LoginViewModel : ViewModel() {
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    // Corregido: asteriscos (*) por guiones bajos (_)
    private val _loginResult = MutableLiveData<Result<JSONObject>>()
    val loginResult: LiveData<Result<JSONObject>> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val TAG = "LoginViewModel"

    // Mejorado: Agregar un interceptor para ver todos los detalles de las peticiones HTTP
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)  // Añadido para mejor depuración
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun testAllUrls() {
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            Log.d(TAG, "INICIANDO PRUEBA DE TODAS LAS URLS")

            val baseUrls = listOf(
                "http://192.168.1.18:8080",
                "http://192.168.1.18:8080/AE_BYD",
                "http://192.168.1.18:8080/AE_BYD-1.0-SNAPSHOT",
                "http://localhost:8080",
                "http://localhost:8080/AE_BYD",
                "http://localhost:8080/AE_BYD-1.0-SNAPSHOT"
            )

            // Primero probar las URLs base con GET
            for (baseUrl in baseUrls) {
                try {
                    val request = Request.Builder()
                        .url(baseUrl)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    Log.d(TAG, "GET a $baseUrl: ${response.code}")

                    // Si no es 404, registrarlo especialmente
                    if (response.code != 404) {
                        Log.d(TAG, "¡¡¡URL base con respuesta diferente a 404: $baseUrl!!!")

                        // Si encontramos una URL base que responde, probamos endpoints debajo
                        val paths = listOf(
                            "/api",
                            "/api/usuario",
                            "/usuario",
                            "/api/login",
                            "/login"
                        )

                        for (path in paths) {
                            try {
                                val pathUrl = baseUrl + path
                                val pathRequest = Request.Builder()
                                    .url(pathUrl)
                                    .get()
                                    .build()

                                val pathResponse = client.newCall(pathRequest).execute()
                                Log.d(TAG, "GET a $pathUrl: ${pathResponse.code}")

                                if (pathResponse.code != 404) {
                                    Log.d(TAG, "¡¡¡URL con subpath con respuesta diferente a 404: $pathUrl!!!")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error probando $baseUrl$path: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error probando $baseUrl: ${e.message}")
                }
            }

            Log.d(TAG, "PRUEBA DE URLS COMPLETADA")
        }
    }

    fun login() {
        if (email.value.isNullOrEmpty() || password.value.isNullOrEmpty()) {
            _loginResult.postValue(Result.failure(Exception("Por favor, complete todos los campos")))
            return
        }

        _isLoading.postValue(true)

        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                // Usar la URL que sabemos que funciona
                val url = "http://192.168.1.18:8080/ae_byd/api/usuario/login"

                // Crear el JSON con los datos de login
                val json = JSONObject().apply {
                    put("email", email.value)
                    put("password", password.value)
                }

                val mediaType = "application/json".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "Enviando solicitud a: $url")
                Log.d(TAG, "Cuerpo de la solicitud: ${json.toString()}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Código de respuesta: ${response.code}")
                Log.d(TAG, "Cuerpo de respuesta: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        _loginResult.postValue(Result.success(jsonResponse))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar respuesta JSON: ${e.message}")
                        _loginResult.postValue(Result.failure(Exception("Error al procesar respuesta: ${e.message}")))
                    }
                } else {
                    Log.e(TAG, "Error del servidor: Código ${response.code}")

                    // Intentar extraer más detalles del error
                    val errorMessage = if (responseBody != null && responseBody.isNotEmpty()) {
                        try {
                            val jsonError = JSONObject(responseBody)
                            jsonError.optString("message", "Error del servidor")
                        } catch (e: Exception) {
                            responseBody
                        }
                    } else {
                        "Error del servidor: Código ${response.code}"
                    }

                    _loginResult.postValue(Result.failure(Exception(errorMessage)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                e.printStackTrace()
                _loginResult.postValue(Result.failure(Exception("Error de conexión: ${e.message}")))
            } finally {
                _isLoading.postValue(false)
                executorService.shutdown()
                try {
                    executorService.awaitTermination(3, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error al esperar terminación del executor: ${e.message}")
                }
            }
        }
    }}