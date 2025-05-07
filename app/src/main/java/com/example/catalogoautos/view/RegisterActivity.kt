package com.example.catalogoautos.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.catalogoautos.databinding.ActivityRegisterBinding
import com.example.catalogoautos.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding // Usa la clase generada por ViewBinding
    private val vm: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura el botón de registro
        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString()
            val apellido = binding.etApellido.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // Validaciones básicas
            if (nombre.isEmpty()) {
                binding.etNombre.error = "El nombre es obligatorio"
                return@setOnClickListener
            }
            if (apellido.isEmpty()) {
                binding.etApellido.error = "El apellido es obligatorio"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.etEmail.error = "El email es obligatorio"
                return@setOnClickListener
            }
            if (!email.endsWith("@admin.com") && !email.endsWith("@tec.com")) {
                binding.etEmail.error = "El email debe terminar con @admin.com o @tec.com"
                return@setOnClickListener
            }
            if (password.length < 8) {
                binding.etPassword.error = "La contraseña debe tener al menos 8 caracteres"
                return@setOnClickListener
            }

            // Asignamos los valores al ViewModel
            vm.nombre.value = nombre
            vm.apellido.value = apellido
            vm.email.value = email
            vm.password.value = password

            // Llamamos a la función para registrar al usuario
            vm.register()
        }

        // Observamos el estado de carga
        vm.isLoading.observe(this) { isLoading ->
            // Mostrar/ocultar indicador de carga
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Habilitar/deshabilitar botón de registro
            binding.btnRegister.isEnabled = !isLoading
        }

        // Observamos el resultado del registro
        vm.result.observe(this) { result ->
            result.onSuccess {
                // En caso de éxito, mostramos mensaje y volvemos al login
                Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_LONG).show()
                finish()  // Cerrar esta actividad
            }.onFailure { error ->
                // En caso de error, mostramos mensaje de error
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}