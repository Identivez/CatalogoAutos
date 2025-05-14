package com.example.catalogoautos.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiClient {

    // URL base centralizada - solo hay que cambiarla aquí
    private const val BASE_URL = "http://10.250.3.8:8080/AE_BYD/api/"

    // Constantes para los endpoints específicos
    const val AUTO_ENDPOINT = "auto"
    const val USER_ENDPOINT = "usuario"
    const val LOGIN_ENDPOINT = "$USER_ENDPOINT/login"

    // Para obtener la URL completa de un endpoint
    fun getFullUrl(endpoint: String): String = "$BASE_URL$endpoint"

    // Crear un interceptor para logging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Crear el cliente OkHttp con el interceptor y timeouts
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Crear Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    // Crear servicios
    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val autoApi: AutoApi = retrofit.create(AutoApi::class.java)

    // Método para obtener la URL base (útil para depuración)
    fun getBaseUrl(): String {
        return BASE_URL
    }
}