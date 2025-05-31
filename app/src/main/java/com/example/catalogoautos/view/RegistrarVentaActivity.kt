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


        supportActionBar?.apply {
            title = "Registrar Venta"

            setDisplayHomeAsUpEnabled(true)
        }


        viewModel = ViewModelProvider(this, VentasViewModel.Factory(application))[VentasViewModel::class.java]


        autoRepository = AutoRepository.getInstance(applicationContext)


        viewModel.limpiarMensajes()


        binding.btnRegistrarVenta.setOnClickListener {
            registrarVenta()
        }


        observeViewModel()


        if (intent.hasExtra("NUMERO_SERIE")) {
            binding.etNumeroSerie.setText(intent.getStringExtra("NUMERO_SERIE"))

            binding.etCantidad.requestFocus()
        }
    }

    private fun observeViewModel() {

        viewModel.isLoading.observe(this) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
            binding.btnRegistrarVenta.isEnabled = !estaCargando


            binding.etNumeroSerie.isEnabled = !estaCargando
            binding.etCantidad.isEnabled = !estaCargando
            binding.etPrecio.isEnabled = !estaCargando
        }


        viewModel.error.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }


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

        val nSerie = binding.etNumeroSerie.text.toString().trim()
        val cantidadStr = binding.etCantidad.text.toString().trim()
        val precioStr = binding.etPrecio.text.toString().trim()


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


            val auto = autoRepository.buscarPorNumeroSerie(nSerie)
            if (auto == null) {
                binding.etNumeroSerie.error = "Número de serie no encontrado"
                binding.etNumeroSerie.requestFocus()
                return
            }


            if (!auto.disponibilidad) {
                binding.etNumeroSerie.error = "Este auto no está disponible para venta"
                binding.etNumeroSerie.requestFocus()
                return
            }


            if (auto.stock < cantidad) {
                binding.etCantidad.error = "Stock insuficiente. Disponible: ${auto.stock}"
                binding.etCantidad.requestFocus()
                return
            }


            viewModel.registrarVenta(nSerie, cantidad, precio)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Formato de número inválido", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onBackPressed() {
        if (binding.etNumeroSerie.text.isNotEmpty() ||
            binding.etCantidad.text.isNotEmpty() ||
            binding.etPrecio.text.isNotEmpty()) {

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