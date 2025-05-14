// RegistrarVentaActivity.kt
package com.example.catalogoautos.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.databinding.ActivityRegistrarVentaBinding
import com.example.catalogoautos.viewmodel.VentasViewModel
import java.math.BigDecimal

class RegistrarVentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarVentaBinding
    private lateinit var viewModel: VentasViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarVentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ocultar la barra de acción o configurar título
        supportActionBar?.title = "Registrar Venta"

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, VentasViewModel.Factory(application))[VentasViewModel::class.java]

        // Configurar botón de registro
        binding.btnRegistrarVenta.setOnClickListener {
            registrarVenta()
        }

        // Observar ViewModel
        observeViewModel()
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
            binding.btnRegistrarVenta.isEnabled = !estaCargando
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

            // Registrar la venta
            viewModel.registrarVenta(nSerie, cantidad, precio)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Formato de número inválido", Toast.LENGTH_SHORT).show()
        }
    }
}