// DetalleVentaActivity.kt (continuación)
package com.example.catalogoautos.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.databinding.ActivityDetalleVentaBinding
import com.example.catalogoautos.model.Venta
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

        // Ocultar la barra de acción o configurar título
        supportActionBar?.title = "Detalle de Venta"

        // Obtener el ID de la venta
        ventaId = intent.getIntExtra("VENTA_ID", 0)
        if (ventaId == 0) {
            Toast.makeText(this, "Error: ID de venta no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, VentasViewModel.Factory(application))[VentasViewModel::class.java]

        // Configurar spinner de estados
        val estados = arrayOf("COMPLETADA", "ENTREGADA", "PENDIENTE", "CANCELADA")
        val adapterEstados = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEstado.adapter = adapterEstados

        // Configurar botones
        binding.btnActualizarEstado.setOnClickListener {
            actualizarEstado()
        }

        binding.btnCancelarVenta.setOnClickListener {
            confirmarCancelacion()
        }

        // Observar datos
        observeViewModel()

        // Cargar la venta
        viewModel.obtenerVentaPorId(ventaId)
    }

    private fun observeViewModel() {
        // Observar venta
        viewModel.venta.observe(this) { venta ->
            if (venta != null) {
                mostrarDatosVenta(venta)
            }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
            binding.btnActualizarEstado.isEnabled = !estaCargando
            binding.btnCancelarVenta.isEnabled = !estaCargando
        }

        // Observar errores
        viewModel.error.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }

        // Observar éxito
        viewModel.success.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                // Recargar la venta para ver los cambios
                viewModel.obtenerVentaPorId(ventaId)
            }
        }
    }

    private fun mostrarDatosVenta(venta: Venta) {
        // Formatear datos
        val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        // Mostrar datos en las vistas
        binding.tvNumeroSerie.text = venta.nSerie
        binding.tvCantidad.text = venta.cantidad.toString()
        binding.tvPrecio.text = formatoPrecio.format(venta.precio)
        binding.tvFechaVenta.text = venta.fechaVenta.format(formatoFecha)
        binding.tvEstadoActual.text = venta.estatus

        // Actualizar el spinner al estado actual
        val estados = arrayOf("COMPLETADA", "ENTREGADA", "PENDIENTE", "CANCELADA")
        val posicionEstado = estados.indexOf(venta.estatus)
        if (posicionEstado >= 0) {
            binding.spinnerEstado.setSelection(posicionEstado)
        }

        // Deshabilitar botón de cancelar si ya está cancelada
        binding.btnCancelarVenta.isEnabled = venta.estatus != "CANCELADA"

        // Configurar colores según el estado
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

        // Verificar si el estado ha cambiado
        if (nuevoEstado == estadoActual) {
            Toast.makeText(this, "El estado no ha cambiado", Toast.LENGTH_SHORT).show()
            return
        }

        // Confirmar cambio de estado
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