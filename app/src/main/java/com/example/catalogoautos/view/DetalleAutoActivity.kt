package com.example.catalogoautos.view

import android.content.Intent
import android.net.Uri
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
    private lateinit var tvEstado: TextView
    private lateinit var tvKilometraje: TextView
    private lateinit var tvDetallesTecnicos: TextView
    private lateinit var tvFechaRegistro: TextView
    private lateinit var btnEditar: Button

    private lateinit var viewModel: DetalleAutoViewModel
    private var autoId: String? = null
    private var fromCatalogo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_auto)

        // Habilitar botón de volver en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, DetalleAutoViewModel.Factory(this))
            .get(DetalleAutoViewModel::class.java)

        // Obtener ID del auto a mostrar y verificar si viene del catálogo
        autoId = intent.getStringExtra("auto_id") ?: intent.getStringExtra("AUTO_ID")
        fromCatalogo = intent.getBooleanExtra("from_catalogo", false)

        if (autoId == null) {
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
        tvEstado = findViewById(R.id.tvEstado)
        tvKilometraje = findViewById(R.id.tvKilometraje)
        tvDetallesTecnicos = findViewById(R.id.tvDetallesTecnicos)
        btnEditar = findViewById(R.id.btnEditar)

        // Buscar la vista para fecha de registro si existe en el layout
        tvFechaRegistro = findViewById(R.id.tvFechaRegistro) ?: TextView(this)
    }

    private fun configurarModoVisualizacion() {
        // Ocultar botón de edición cuando se viene del catálogo
        btnEditar.visibility = View.GONE

        // Cambiar el título para indicar modo visualización
        supportActionBar?.title = "Detalles del Auto"
    }

    private fun cargarDatosAuto() {
        val auto = viewModel.obtenerAuto(autoId!!)
        if (auto != null) {
            // Cargar imagen si existe
            if (auto.fotoPath.isNotEmpty()) {
                try {
                    ivFotoAuto.setImageURI(Uri.parse(auto.fotoPath))
                } catch (e: Exception) {
                    ivFotoAuto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivFotoAuto.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Mostrar datos del auto
            tvMarca.text = auto.marca
            tvModelo.text = auto.modelo
            tvAño.text = auto.año.toString()
            tvColor.text = auto.color

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            tvEstado.text = auto.estado
            tvKilometraje.text = "${formatoNumeroConSeparadores(auto.kilometraje)} km"
            tvDetallesTecnicos.text = auto.detallesTecnicos

            // Mostrar fecha de registro si la vista existe
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvFechaRegistro.text = dateFormat.format(auto.fechaRegistro)
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
        if (!fromCatalogo) {
            // Solo recargamos los datos si no estamos en modo visualización del catálogo
            cargarDatosAuto()
        }
    }
}