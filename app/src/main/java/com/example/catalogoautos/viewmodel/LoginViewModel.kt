package com.example.catalogoautos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catalogoautos.network.ApiClient
import com.example.catalogoautos.network.NetworkUtils
import com.example.catalogoautos.repository.UsuarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    private val usuarioRepository = UsuarioRepository(application.applicationContext)

    private val TAG = "LoginViewModel"

    fun login() {
        Log.d(TAG, "Iniciando proceso de login")


        if (email.value.isNullOrEmpty() || password.value.isNullOrEmpty()) {
            Log.d(TAG, "Campos obligatorios vacíos")
            _loginResult.postValue(Result.failure(Exception("Por favor, complete todos los campos")))
            return
        }

        _isLoading.value = true


        val loginData = mapOf(
            "email" to email.value!!,
            "password" to password.value!!
        )

        Log.d(TAG, "Iniciando petición al servidor con email: ${email.value}")

        viewModelScope.launch {
            try {

                val resultado = NetworkUtils.post(ApiClient.LOGIN_ENDPOINT, loginData)

                resultado.fold(
                    onSuccess = { responseBody ->
                        try {
                            Log.d(TAG, "Respuesta exitosa del servidor, procesando JSON")

                            if (responseBody.isNullOrEmpty()) {
                                Log.e(TAG, "Respuesta vacía del servidor")
                                _loginResult.postValue(Result.failure(Exception("Respuesta vacía del servidor")))
                                return@fold
                            }

                            Log.d(TAG, "JSON recibido: $responseBody")

                            val jsonResponse = JSONObject(responseBody)


                            val usuarioId = jsonResponse.optInt("usuario_id", 0)  // Cambiado de "usuarioId" a "usuario_id"
                            val nombre = jsonResponse.optString("nombre", "")
                            val apellido = jsonResponse.optString("apellido", "")
                            val email = jsonResponse.optString("email", "")
                            val rol = jsonResponse.optString("rol", "")
                            val fechaRegistro = jsonResponse.optString("fecha_registro", "")  // Cambiado de "fechaRegistro" a "fecha_registro"

                            if (usuarioId == 0) {
                                Log.e(TAG, "ID de usuario no válido en la respuesta")
                                _loginResult.postValue(Result.failure(Exception("Datos de usuario no válidos")))
                                return@fold
                            }

                            Log.d(TAG, "Datos extraídos del JSON: ID=$usuarioId, Nombre=$nombre, Apellido=$apellido")


                            try {
                                usuarioRepository.setUsuarioActual(
                                    usuarioId, nombre, apellido, email, rol, fechaRegistro
                                )
                                Log.d(TAG, "Usuario guardado en sesión: $nombre $apellido")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al guardar usuario en sesión: ${e.message}", e)
                                _loginResult.postValue(Result.failure(Exception("Error al guardar datos de usuario: ${e.message}")))
                                return@fold
                            }

                            _loginResult.postValue(Result.success(Unit))

                        } catch (e: JSONException) {

                            Log.e(TAG, "Error al procesar JSON: ${e.message}", e)


                            Log.e(TAG, "JSON que causó el error: $responseBody")

                            _loginResult.postValue(Result.failure(Exception("Error al procesar la respuesta del servidor: ${e.message}")))
                        } catch (e: Exception) {
                            // Cualquier otro error al procesar la respuesta
                            Log.e(TAG, "Error al procesar respuesta: ${e.message}", e)
                            _loginResult.postValue(Result.failure(Exception("Error al procesar respuesta: ${e.message}")))
                        }
                    },
                    onFailure = { error ->

                        val mensaje = when (error) {
                            is UnknownHostException -> "No se pudo conectar al servidor. Verifique su conexión a Internet."
                            is ConnectException -> "Error de conexión al servidor. El servidor puede estar caído."
                            is SocketTimeoutException -> "La conexión con el servidor ha tardado demasiado. Inténtelo nuevamente."
                            else -> "Error en el inicio de sesión: ${error.message}"
                        }

                        Log.e(TAG, "Error en la petición de login: $mensaje", error)
                        _loginResult.postValue(Result.failure(Exception(mensaje)))
                    }
                )
            } catch (e: CancellationException) {

                throw e
            } catch (e: Exception) {

                Log.e(TAG, "Error general de conexión: ${e.message}", e)
                _loginResult.postValue(Result.failure(Exception("Error de conexión: ${e.message}")))
            } finally {

                _isLoading.postValue(false)
                Log.d(TAG, "Proceso de login finalizado")
            }
        }
    }


    private fun extractUserId(jsonResponse: JSONObject): Int {

        val usuarioId = when {
            jsonResponse.has("usuario_id") -> jsonResponse.optInt("usuario_id", 0)
            jsonResponse.has("usuarioId") -> jsonResponse.optInt("usuarioId", 0)
            else -> 0
        }

        if (usuarioId == 0) {

            val keys = jsonResponse.keys()
            val availableFields = mutableListOf<String>()
            while (keys.hasNext()) {
                availableFields.add(keys.next())
            }
            Log.d(TAG, "Campos disponibles en el JSON: $availableFields")
        }

        return usuarioId
    }


    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}