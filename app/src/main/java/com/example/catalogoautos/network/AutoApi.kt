package com.example.catalogoautos.network

import retrofit2.Response
import retrofit2.http.*

// Definir un tipo específico para el mapa
typealias AutoMap = HashMap<String, Any>

interface AutoApi {
    @GET("auto")
    suspend fun obtenerTodos(): Response<List<AutoMap>>

    @GET("auto/{id}")
    suspend fun obtenerPorId(@Path("id") id: Int): Response<AutoMap>

    @POST("auto")
    suspend fun agregarAuto(@Body auto: AutoMap): Response<AutoMap>

    @PUT("auto/{id}")
    suspend fun actualizarAuto(@Path("id") id: Int, @Body auto: AutoMap): Response<AutoMap>

    @DELETE("auto/{id}")
    suspend fun eliminarAuto(@Path("id") id: Int): Response<Void>
}