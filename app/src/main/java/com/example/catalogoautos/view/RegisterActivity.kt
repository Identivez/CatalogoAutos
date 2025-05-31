package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.catalogoautos.databinding.ActivityRegisterBinding
import com.example.catalogoautos.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val vm: RegisterViewModel by viewModels()
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "RegisterActivity onCreate")

        try {

            binding = ActivityRegisterBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.btnRegister.setOnClickListener {
                try {
                    validarYRegistrar()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al validar o registrar: ${e.message}", e)
                    Toast.makeText(this, "Error en el registro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }


            vm.isLoading.observe(this) { isLoading ->
                try {
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

                    binding.btnRegister.isEnabled = !isLoading
                } catch (e: Exception) {
                    Log.e(TAG, "Error al manejar estado de carga: ${e.message}", e)
                }
            }


            vm.result.observe(this) { result ->
                try {
                    result.onSuccess {
                        Log.d(TAG, "Registro exitoso")

                        Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_LONG).show()

                        try {

                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al navegar después del registro exitoso: ${e.message}", e)

                            finish()
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "Error en el registro: ${error.message}", error)

                        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar resultado del registro: ${e.message}", e)
                    Toast.makeText(this, "Error interno: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun validarYRegistrar() {
        val nombre = binding.etNombre.text.toString()
        val apellido = binding.etApellido.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()


        if (nombre.isEmpty()) {
            binding.etNombre.error = "El nombre es obligatorio"
            return
        }
        if (apellido.isEmpty()) {
            binding.etApellido.error = "El apellido es obligatorio"
            return
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "El email es obligatorio"
            return
        }
        if (!email.endsWith("@admin.com") && !email.endsWith("@tec.com")) {
            binding.etEmail.error = "El email debe terminar con @admin.com o @tec.com"
            return
        }
        if (password.length < 8) {
            binding.etPassword.error = "La contraseña debe tener al menos 8 caracteres"
            return
        }


        Log.d(TAG, "Datos validados correctamente. Procediendo con el registro")


        vm.nombre.value = nombre
        vm.apellido.value = apellido
        vm.email.value = email
        vm.password.value = password


        vm.register()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "RegisterActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "RegisterActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "RegisterActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RegisterActivity onDestroy")
    }
}