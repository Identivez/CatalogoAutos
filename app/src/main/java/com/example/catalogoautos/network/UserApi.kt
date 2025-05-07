package com.example.catalogoautos.network

import com.example.catalogoautos.model.Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("usuario")
    suspend fun register(@Body usuario: Usuario): Response<Void>

    //@POST("usuario/login")
    //suspend fun login(@Body credentials: Credentials): Response<Usuario>
}
