package com.example.catalogoautos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.repository.UsuarioRepository

class LoginViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {

    fun login(username: String, password: String): Boolean {
        return usuarioRepository.login(username, password)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(UsuarioRepository.getInstance()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
