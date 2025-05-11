package com.example.catalogoautos.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("usuario")
    suspend fun register(@Body usuario: Map<String, String>): Response<Void>

    @POST("usuario/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<Map<String, Any>>
}