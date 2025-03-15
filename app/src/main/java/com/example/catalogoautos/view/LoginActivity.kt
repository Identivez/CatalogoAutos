package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, LoginViewModel.Factory())
            .get(LoginViewModel::class.java)

        // Configurar referencias a vistas
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)

        // Configurar click listener para el botón de login
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Por favor, complete todos los campos.")
                return@setOnClickListener
            }

            if (viewModel.login(username, password)) {
                // Login exitoso, navegar al menú principal
                startActivity(Intent(this, MenuActivity::class.java))
                finish() // Cerrar esta actividad para que no puedan volver atrás
            } else {
                showError("Usuario o contraseña incorrectos.")
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}