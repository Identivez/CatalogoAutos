package com.example.catalogoautos.view

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.databinding.ActivityRegistrarVentaBinding
import com.example.catalogoautos.repository.AutoRepository
import com.example.catalogoautos.viewmodel.VentasViewModel
import java.math.BigDecimal

class RegistrarVentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarVentaBinding
    private lateinit var viewModel: VentasViewModel
    private lateinit var autoRepository: AutoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarVentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la barra de acción
        supportActionBar?.apply {
            title = "Registrar Venta"
            // Habilitar el botón de regreso
            setDisplayHomeAsUpEnabled(true)
        }

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, VentasViewModel.Factory(application))[VentasViewModel::class.java]

        // Inicializar el repositorio de autos para validaciones
        autoRepository = AutoRepository.getInstance(applicationContext)

        // Limpiar mensajes anteriores al iniciar
        viewModel.limpiarMensajes()

        // Configurar botón de registro
        binding.btnRegistrarVenta.setOnClickListener {
            registrarVenta()
        }

        // Observar ViewModel
        observeViewModel()

        // Si hay un número de serie pasado como extra, llenarlo automáticamente
        if (intent.hasExtra("NUMERO_SERIE")) {
            binding.etNumeroSerie.setText(intent.getStringExtra("NUMERO_SERIE"))
            // Focus en el siguiente campo
            binding.etCantidad.requestFocus()
        }
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
            binding.btnRegistrarVenta.isEnabled = !estaCargando

            // También deshabilitar campos durante la carga
            binding.etNumeroSerie.isEnabled = !estaCargando
            binding.etCantidad.isEnabled = !estaCargando
            binding.etPrecio.isEnabled = !estaCargando
        }

        // Observar errores
        viewModel.error.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }

        // Observar éxito en el registro
        viewModel.success.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                // Cerrar actividad después de registrar exitosamente
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun registrarVenta() {
        // Obtener valores de los campos
        val nSerie = binding.etNumeroSerie.text.toString().trim()
        val cantidadStr = binding.etCantidad.text.toString().trim()
        val precioStr = binding.etPrecio.text.toString().trim()

        // Validar campos
        if (nSerie.isEmpty()) {
            binding.etNumeroSerie.error = "El número de serie es requerido"
            binding.etNumeroSerie.requestFocus()
            return
        }

        if (cantidadStr.isEmpty()) {
            binding.etCantidad.error = "La cantidad es requerida"
            binding.etCantidad.requestFocus()
            return
        }

        if (precioStr.isEmpty()) {
            binding.etPrecio.error = "El precio es requerido"
            binding.etPrecio.requestFocus()
            return
        }

        try {
            val cantidad = cantidadStr.toInt()
            val precio = BigDecimal(precioStr)

            // Validar valores
            if (cantidad <= 0) {
                binding.etCantidad.error = "La cantidad debe ser mayor a cero"
                binding.etCantidad.requestFocus()
                return
            }

            if (precio <= BigDecimal.ZERO) {
                binding.etPrecio.error = "El precio debe ser mayor a cero"
                binding.etPrecio.requestFocus()
                return
            }

            // Verificar que el número de serie exista
            val auto = autoRepository.buscarPorNumeroSerie(nSerie)
            if (auto == null) {
                binding.etNumeroSerie.error = "Número de serie no encontrado"
                binding.etNumeroSerie.requestFocus()
                return
            }

            // Verificar que el auto esté disponible
            if (!auto.disponibilidad) {
                binding.etNumeroSerie.error = "Este auto no está disponible para venta"
                binding.etNumeroSerie.requestFocus()
                return
            }

            // Verificar que haya suficiente stock
            if (auto.stock < cantidad) {
                binding.etCantidad.error = "Stock insuficiente. Disponible: ${auto.stock}"
                binding.etCantidad.requestFocus()
                return
            }

            // Registrar la venta
            viewModel.registrarVenta(nSerie, cantidad, precio)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Formato de número inválido", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Manejar el botón de regreso en la barra de acción
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Mostrar confirmación antes de salir si hay datos ingresados
    override fun onBackPressed() {
        if (binding.etNumeroSerie.text.isNotEmpty() ||
            binding.etCantidad.text.isNotEmpty() ||
            binding.etPrecio.text.isNotEmpty()) {
            // Aquí podrías mostrar un diálogo preguntando si desea descartar los cambios
            // Por simplicidad, solo agrego la lógica básica
            if (viewModel.isLoading.value == true) {
                Toast.makeText(this, "Espere a que se complete la operación", Toast.LENGTH_SHORT).show()
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}