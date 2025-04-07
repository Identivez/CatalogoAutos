package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.DetalleAutoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleAutoActivity : AppCompatActivity() {

    private lateinit var ivFotoAuto: ImageView
    private lateinit var tvMarca: TextView
    private lateinit var tvModelo: TextView
    private lateinit var tvAño: TextView
    private lateinit var tvColor: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvDisponibilidad: TextView
    private lateinit var tvStock: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvFechaRegistro: TextView
    private lateinit var tvFechaActualizacion: TextView
    private lateinit var btnEditar: Button

    private lateinit var viewModel: DetalleAutoViewModel
    private var autoId: Int = -1
    private var fromCatalogo: Boolean = false

    // Mapa temporal de marcas - en una implementación real, esto vendría de tu repositorio de marcas
    private val marcasMap = mapOf(
        1 to "Toyota",
        2 to "Honda",
        3 to "Ford",
        4 to "Chevrolet",
        5 to "Nissan"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_auto)

        // Habilitar botón de volver en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, DetalleAutoViewModel.Factory(this))
            .get(DetalleAutoViewModel::class.java)

        // Obtener ID del auto a mostrar y verificar si viene del catálogo
        autoId = intent.getIntExtra("auto_id", -1)
        if (autoId == -1) {
            autoId = intent.getIntExtra("AUTO_ID", -1)
        }
        fromCatalogo = intent.getBooleanExtra("from_catalogo", false)

        if (autoId == -1) {
            Toast.makeText(this, "Error: No se pudo encontrar el auto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar referencias a vistas
        setupViews()

        // Cargar datos del auto
        cargarDatosAuto()

        // Configurar modo de visualización si viene del catálogo
        if (fromCatalogo) {
            configurarModoVisualizacion()
        } else {
            // Configurar listener del botón de editar
            btnEditar.setOnClickListener {
                val intent = Intent(this, AgregarAutoActivity::class.java)
                intent.putExtra("AUTO_ID", autoId)
                startActivity(intent)
            }
        }
    }

    private fun setupViews() {
        ivFotoAuto = findViewById(R.id.ivFotoAuto)
        tvMarca = findViewById(R.id.tvMarca)
        tvModelo = findViewById(R.id.tvModelo)
        tvAño = findViewById(R.id.tvAño)
        tvColor = findViewById(R.id.tvColor)
        tvPrecio = findViewById(R.id.tvPrecio)
        tvDisponibilidad = findViewById(R.id.tvEstado) // Reusamos esta vista
        tvStock = findViewById(R.id.tvKilometraje) // Reusamos esta vista
        tvDescripcion = findViewById(R.id.tvDetallesTecnicos) // Reusamos esta vista
        btnEditar = findViewById(R.id.btnEditar)

        // Buscar las vistas para fecha de registro y actualización si existen en el layout
        tvFechaRegistro = findViewById(R.id.tvFechaRegistro) ?: TextView(this)
        tvFechaActualizacion = findViewById(R.id.tvFechaActualizacion) ?: TextView(this)
    }

    private fun configurarModoVisualizacion() {
        // Ocultar botón de edición cuando se viene del catálogo
        btnEditar.visibility = View.GONE

        // Cambiar el título para indicar modo visualización
        supportActionBar?.title = "Detalles del Auto"
    }

    private fun cargarDatosAuto() {
        val auto = viewModel.obtenerAuto(autoId)
        if (auto != null) {
            // Para la imagen, en una implementación completa buscarías la foto principal
            // usando la relación con la entidad Foto
            // Por ahora mostramos una imagen predeterminada
            ivFotoAuto.setImageResource(R.drawable.ic_launcher_foreground)

            // Mostrar datos del auto
            tvMarca.text = marcasMap[auto.marca_id] ?: "Marca ID: ${auto.marca_id}"
            tvModelo.text = auto.modelo
            tvAño.text = auto.año.toString()
            tvColor.text = auto.color

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            // Mostrar disponibilidad y stock (usando las vistas reutilizadas)
            tvDisponibilidad.text = if (auto.disponibilidad) "Disponible" else "No disponible"
            tvStock.text = "Stock: ${formatoNumeroConSeparadores(auto.stock)} unidades"
            tvDescripcion.text = auto.descripcion

            // Mostrar fechas si las vistas existen
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvFechaRegistro.text = "Fecha de registro: ${dateFormat.format(auto.fecha_registro)}"
                tvFechaActualizacion.text = "Última actualización: ${dateFormat.format(auto.fecha_actualizacion)}"
            } catch (e: Exception) {
                // La vista puede no existir, no hacemos nada en ese caso
            }
        } else {
            Toast.makeText(this, "Error: No se pudo cargar la información del auto", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Función para formatear números con separadores de miles
    private fun formatoNumeroConSeparadores(numero: Int): String {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(numero)
    }

    // Manejar clic en el botón de volver
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Actualizar datos cuando se regresa de editar
    override fun onResume() {
        super.onResume()
        if (!fromCatalogo && autoId != -1) {
            // Solo recargamos los datos si no estamos en modo visualización del catálogo
            cargarDatosAuto()
        }
    }
}