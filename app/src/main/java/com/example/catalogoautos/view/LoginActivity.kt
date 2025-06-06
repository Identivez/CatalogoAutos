package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var ibTogglePassword: ImageButton
    private lateinit var btnGoToRegister: Button

    private lateinit var viewModel: LoginViewModel
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "LoginActivity onCreate")

        try {
            supportActionBar?.hide()
            setContentView(R.layout.activity_login)


            viewModel = ViewModelProvider(this, LoginViewModel.Factory(application))
                .get(LoginViewModel::class.java)

            etUsername = findViewById(R.id.etUsername)
            etPassword = findViewById(R.id.etPassword)
            btnLogin = findViewById(R.id.btnLogin)
            tvError = findViewById(R.id.tvError)
            ibTogglePassword = findViewById(R.id.ibTogglePassword)
            btnGoToRegister = findViewById(R.id.btnGoToRegister)


            ibTogglePassword.setOnClickListener {
                try {
                    togglePasswordVisibility()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al cambiar visibilidad de contraseña: ${e.message}", e)
                }
            }


            btnLogin.setOnClickListener {
                try {
                    attemptLogin()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al intentar login: ${e.message}", e)
                    Toast.makeText(this, "Error interno: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }


            btnGoToRegister.setOnClickListener {
                try {
                    Log.d(TAG, "Iniciando RegisterActivity")
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar RegisterActivity: ${e.message}", e)
                    Toast.makeText(this, "Error al abrir pantalla de registro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }


            viewModel.loginResult.observe(this) { result ->
                try {
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Login exitoso")
                            hideError()

                            try {

                                val intent = Intent(this@LoginActivity, MenuActivity::class.java)


                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                                Log.d(TAG, "Iniciando MenuActivity desde LoginActivity")


                                startActivity(intent)

                                Log.d(TAG, "Después de startActivity para MenuActivity")


                                finish()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al iniciar MenuActivity: ${e.message}", e)
                                Toast.makeText(this, "Error al abrir menú principal: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error de login: ${error.message}")
                            showError("Error: ${error.message}")


                            Toast.makeText(
                                this,
                                "Error de conexión. Verifica tus credenciales o la conexión al servidor.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar resultado de login: ${e.message}", e)
                    Toast.makeText(this, "Error interno: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }


            viewModel.isLoading.observe(this) { isLoading ->
                try {
                    if (isLoading) {
                        btnLogin.isEnabled = false
                        btnLogin.text = getString(R.string.logging_in)
                        Toast.makeText(this, "Validando credenciales...", Toast.LENGTH_SHORT).show()
                    } else {
                        btnLogin.isEnabled = true
                        btnLogin.text = getString(R.string.login)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar UI durante carga: ${e.message}", e)
                }
            }

            Log.d(TAG, "URL del servidor: http://192.168.1.14:8080/ae_byd/api/usuario/login")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "LoginActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "LoginActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "LoginActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LoginActivity onDestroy")
    }

    private fun togglePasswordVisibility() {
        if (etPassword.transformationMethod is PasswordTransformationMethod) {

            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            ibTogglePassword.setImageResource(R.drawable.ic_visibility)
        } else {

            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            ibTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        }

        etPassword.setSelection(etPassword.text.length)
    }

    private fun attemptLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.login_error_empty_fields))
            return
        }


        hideError()

        Log.d(TAG, "Intentando login con email: $username")


        viewModel.email.value = username
        viewModel.password.value = password


        viewModel.login()
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}