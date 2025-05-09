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

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _result = MutableLiveData<Result<Unit>>()
    val result: LiveData<Result<Unit>> = _result

    // Crear un cliente de OkHttp para hacer las solicitudes de red
    private val client = OkHttpClient()

    // Función para registrar el usuario sin la capa de red adicional
    fun register() {
        // Validar que los campos no estén vacíos
        if (nombre.value.isNullOrEmpty() ||
            apellido.value.isNullOrEmpty() ||
            email.value.isNullOrEmpty() ||
            password.value.isNullOrEmpty()) {
            _result.value = Result.failure(Exception("Todos los campos son obligatorios"))
            return
        }

        // Validar el formato del correo
        if (!email.value!!.endsWith("@admin.com") && !email.value!!.endsWith("@tec.com")) {
            _result.value = Result.failure(Exception("El correo debe terminar con @admin.com o @tec.com"))
            return
        }

        // Validar longitud de la contraseña
        if (password.value!!.length < 8) {
            _result.value = Result.failure(Exception("La contraseña debe tener al menos 8 caracteres"))
            return
        }

        _isLoading.value = true

        // Crear el JSON para enviar al backend
        val json = JSONObject().apply {
            put("nombre", nombre.value)
            put("apellido", apellido.value)
            put("email", email.value)
            put("password", password.value)
        }

        // Crear la solicitud HTTP POST
        val mediaType = "application/json".toMediaType()
        val body = RequestBody.create(mediaType, json.toString())
        val request = Request.Builder()
            .url("http://10.250.3.8:8080/ae_byd/api/usuario")  // URL actualizada con la IP correcta
            .post(body)
            .build()

        // Realizar la solicitud asíncrona
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _isLoading.postValue(false)
                _result.postValue(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                _isLoading.postValue(false)

                if (response.isSuccessful) {
                    _result.postValue(Result.success(Unit))  // Registro exitoso
                } else {
                    // Intentar obtener el mensaje de error del cuerpo de la respuesta
                    val errorBody = response.body?.string() ?: "Error desconocido"
                    _result.postValue(Result.failure(Exception(errorBody)))
                }

                response.close()
            }
        })
    }
}