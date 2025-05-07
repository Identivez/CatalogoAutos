package com.example.catalogoautos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class RegisterViewModel : ViewModel() {

    val nombre = MutableLiveData<String>()
    val apellido = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    private val _result = MutableLiveData<Result<Unit>>()
    val result: LiveData<Result<Unit>> = _result

    // Crear un cliente de OkHttp para hacer las solicitudes de red
    private val client = OkHttpClient()

    // Función para registrar el usuario sin la capa de red adicional
    fun register() {
        // Crear el JSON para enviar al backend
        val json = JSONObject().apply {
            put("nombre", nombre.value.orEmpty())
            put("apellido", apellido.value.orEmpty())
            put("email", email.value.orEmpty())
            put("password", password.value.orEmpty())
        }

        // Crear la solicitud HTTP POST
        val mediaType = "application/json".toMediaType()  // Cambiado a la nueva función de extensión
        val body = RequestBody.create(mediaType, json.toString())
        val request = Request.Builder()
            .url("http://localhost:8080/AE_BYD/api/usuario")  // URL de tu backend
            .post(body)
            .build()

        // Realizar la solicitud asíncrona
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _result.postValue(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    _result.postValue(Result.success(Unit))  // Registro exitoso
                } else {
                    _result.postValue(Result.failure(Exception("Error al registrar usuario")))
                }
            }
        })
    }
}
