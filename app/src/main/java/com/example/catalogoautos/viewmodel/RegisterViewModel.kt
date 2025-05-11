package com.example.catalogoautos.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.network.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegisterViewModel : ViewModel() {

    val nombre = MutableLiveData<String>()
    val apellido = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _result = MutableLiveData<Result<Unit>>()
    val result: LiveData<Result<Unit>> = _result

    val TAG = "RegisterViewModel"

    fun register() {
        // Validar campos
        if (nombre.value.isNullOrEmpty() ||
            apellido.value.isNullOrEmpty() ||
            email.value.isNullOrEmpty() ||
            password.value.isNullOrEmpty()) {
            _result.value = Result.failure(Exception("Todos los campos son obligatorios"))
            return
        }

        // Validar email
        if (!email.value!!.endsWith("@admin.com") && !email.value!!.endsWith("@tec.com")) {
            _result.value = Result.failure(Exception("El correo debe terminar con @admin.com o @tec.com"))
            return
        }

        // Validar password
        if (password.value!!.length < 8) {
            _result.value = Result.failure(Exception("La contraseña debe tener al menos 8 caracteres"))
            return
        }

        _isLoading.value = true

        // Crear un mapa con los datos del usuario
        val usuarioMap = mapOf(
            "nombre" to nombre.value!!,
            "apellido" to apellido.value!!,
            "email" to email.value!!,
            "password" to password.value!!
        )

        // Usar coroutines para la llamada a la API
        viewModelScope.launch {
            try {
                val response = ApiClient.userApi.register(usuarioMap)

                if (response.isSuccessful) {
                    _result.value = Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    _result.value = Result.failure(Exception(errorBody))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en la llamada API: ${e.message}")
                _result.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}