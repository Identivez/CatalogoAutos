// DetalleVentaActivity.kt (continuación)
package com.example.catalogoautos.view

import Venta
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.databinding.ActivityDetalleVentaBinding

import com.example.catalogoautos.viewmodel.VentasViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetalleVentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleVentaBinding
    private lateinit var viewModel: VentasViewModel
    private var ventaId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleVentaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        supportActionBar?.title = "Detalle de Venta"


        ventaId = intent.getIntExtra("VENTA_ID", 0)
        if (ventaId == 0) {
            Toast.makeText(this, "Error: ID de venta no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        viewModel = ViewModelProvider(this, VentasViewModel.Factory(application))[VentasViewModel::class.java]


        val estados = arrayOf("COMPLETADA", "ENTREGADA", "PENDIENTE", "CANCELADA")
        val adapterEstados = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEstado.adapter = adapterEstados


        binding.btnActualizarEstado.setOnClickListener {
            actualizarEstado()
        }

        binding.btnCancelarVenta.setOnClickListener {
            confirmarCancelacion()
        }


        observeViewModel()


        viewModel.obtenerVentaPorId(ventaId)
    }

    private fun observeViewModel() {

        viewModel.venta.observe(this) { venta ->
            if (venta != null) {
                mostrarDatosVenta(venta)
            }
        }
        viewModel.isLoading.observe(this) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
            binding.btnActualizarEstado.isEnabled = !estaCargando
            binding.btnCancelarVenta.isEnabled = !estaCargando
        }


        viewModel.error.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }


        viewModel.success.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                // Recargar la venta para ver los cambios
                viewModel.obtenerVentaPorId(ventaId)
            }
        }
    }

    private fun mostrarDatosVenta(venta: Venta) {

        val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")


        binding.tvNumeroSerie.text = venta.nSerie
        binding.tvCantidad.text = venta.cantidad.toString()
        binding.tvPrecio.text = formatoPrecio.format(venta.precio)
        binding.tvFechaVenta.text = venta.fechaVenta.format(formatoFecha)
        binding.tvEstadoActual.text = venta.estatus


        val estados = arrayOf("COMPLETADA", "ENTREGADA", "PENDIENTE", "CANCELADA")
        val posicionEstado = estados.indexOf(venta.estatus)
        if (posicionEstado >= 0) {
            binding.spinnerEstado.setSelection(posicionEstado)
        }


        binding.btnCancelarVenta.isEnabled = venta.estatus != "CANCELADA"


        when (venta.estatus) {
            "COMPLETADA" -> binding.tvEstadoActual.setTextColor(getColor(android.R.color.holo_green_dark))
            "PENDIENTE" -> binding.tvEstadoActual.setTextColor(getColor(android.R.color.holo_orange_dark))
            "CANCELADA" -> binding.tvEstadoActual.setTextColor(getColor(android.R.color.holo_red_dark))
            else -> binding.tvEstadoActual.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun actualizarEstado() {
        val nuevoEstado = binding.spinnerEstado.selectedItem.toString()
        val estadoActual = binding.tvEstadoActual.text.toString()


        if (nuevoEstado == estadoActual) {
            Toast.makeText(this, "El estado no ha cambiado", Toast.LENGTH_SHORT).show()
            return
        }


        AlertDialog.Builder(this)
            .setTitle("Confirmar cambio")
            .setMessage("¿Estás seguro de cambiar el estado a '$nuevoEstado'?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.actualizarEstatus(ventaId, nuevoEstado)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmarCancelacion() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar venta")
            .setMessage("¿Estás seguro de cancelar esta venta? Esta acción no se puede deshacer y el stock del auto será restaurado.")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.cancelarVenta(ventaId)
            }
            .setNegativeButton("No", null)
            .show()
    }
}