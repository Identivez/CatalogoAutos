package com.example.catalogoautos.network

import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    /**
     * Registrar un nuevo usuario
     */
    @POST("usuario")
    suspend fun register(@Body usuario: Map<String, String>): Response<String>

    /**
     * Iniciar sesión
     */
    @POST("usuario/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<Map<String, Any>>

    /**
     * Obtener un usuario por ID
     */
    @GET("usuario/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<Map<String, Any>>

    /**
     * Obtener todos los usuarios
     */
    @GET("usuario")
    suspend fun getAllUsers(): Response<List<Map<String, Any>>>

    /**
     * Obtener la cantidad de usuarios
     */
    @GET("usuario/count")
    suspend fun countUsers(): Response<String>
}