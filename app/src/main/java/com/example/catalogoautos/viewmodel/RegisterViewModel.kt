package com.example.catalogoautos.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.network.ApiClient
import com.example.catalogoautos.network.NetworkUtils
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    val nombre = MutableLiveData<String>()
    val apellido = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _result = MutableLiveData<Result<Unit>>()
    val result: LiveData<Result<Unit>> = _result

    private val TAG = "RegisterViewModel"

    fun register() {

        if (nombre.value.isNullOrEmpty() ||
            apellido.value.isNullOrEmpty() ||
            email.value.isNullOrEmpty() ||
            password.value.isNullOrEmpty()) {
            _result.value = Result.failure(Exception("Todos los campos son obligatorios"))
            return
        }
        if (!email.value!!.endsWith("@admin.com") && !email.value!!.endsWith("@tec.com")) {
            _result.value = Result.failure(Exception("El correo debe terminar con @admin.com o @tec.com"))
            return
        }


        if (password.value!!.length < 8) {
            _result.value = Result.failure(Exception("La contraseña debe tener al menos 8 caracteres"))
            return
        }

        _isLoading.value = true


        val usuarioMap = mapOf(
            "nombre" to nombre.value!!,
            "apellido" to apellido.value!!,
            "email" to email.value!!,
            "password" to password.value!!
        )


        viewModelScope.launch {
            try {

                val resultado = NetworkUtils.post(ApiClient.USER_ENDPOINT, usuarioMap)

                resultado.fold(
                    onSuccess = {
                        Log.d(TAG, "Registro exitoso")
                        _result.value = Result.success(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error en el registro: ${error.message}")
                        _result.value = Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error en la llamada API: ${e.message}")
                _result.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}