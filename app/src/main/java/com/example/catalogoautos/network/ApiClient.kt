package com.example.catalogoautos.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiClient {
    // URL corregida - usando la ruta correcta
    private const val BASE_URL = "http://192.168.1.14:8080/api/"

    // Crear un interceptor para logging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Crear el cliente OkHttp con el interceptor y timeouts
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Crear Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    // Crear servicios
    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val autoApi: AutoApi = retrofit.create(AutoApi::class.java)

    // Método para obtener la URL base (útil para depuración)
    fun getBaseUrl(): String {
        return BASE_URL
    }
}