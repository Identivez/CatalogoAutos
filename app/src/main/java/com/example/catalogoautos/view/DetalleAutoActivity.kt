package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.repository.AutoRepository
import com.example.catalogoautos.viewmodel.DetalleAutoViewModel

class DetalleAutoActivity : AppCompatActivity() {

    // Referencias a las vistas
    private lateinit var ivFotoAuto: ImageView
    private lateinit var tvNumeroSerie: TextView
    private lateinit var tvSku: TextView
    private lateinit var tvMarca: TextView
    private lateinit var tvModelo: TextView
    private lateinit var tvAnio: TextView
    private lateinit var tvColor: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvDisponibilidad: TextView
    private lateinit var tvStock: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvFechaRegistro: TextView
    private lateinit var tvFechaActualizacion: TextView
    private lateinit var btnEditar: Button
    private lateinit var btnCambiarDisponibilidad: Button
    private lateinit var progressBar: View

    // ViewModel
    private lateinit var viewModel: DetalleAutoViewModel

    // Variables auxiliares
    private var autoId: Int = -1
    private var fromCatalogo: Boolean = false
    private val MARCA_BYD = "BYD"
    private val REQUEST_EDIT_AUTO = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_auto)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(
            this,
            DetalleAutoViewModel.Factory(AutoRepository.getInstance(applicationContext))
        ).get(DetalleAutoViewModel::class.java)

        // Configurar barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalles del Auto"

        // Obtener ID del auto a mostrar
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

        // Inicializar vistas
        setupViews()

        // Configurar observadores
        setupObservers()

        // Cargar datos del auto
        viewModel.cargarAuto(autoId)

        // Configurar modo de visualización si viene del catálogo
        if (fromCatalogo) {
            configurarModoVisualizacion()
        } else {
            // Configurar botones de acción
            setupActionButtons()
        }
    }

    private fun setupViews() {
        ivFotoAuto = findViewById(R.id.ivFotoAuto)
        tvNumeroSerie = findViewById(R.id.tvNumeroSerie)
        tvSku = findViewById(R.id.tvSku)
        tvMarca = findViewById(R.id.tvMarca)
        tvModelo = findViewById(R.id.tvModelo)
        tvAnio = findViewById(R.id.tvAnio)
        tvColor = findViewById(R.id.tvColor)
        tvPrecio = findViewById(R.id.tvPrecio)
        tvDisponibilidad = findViewById(R.id.tvEstado)
        tvStock = findViewById(R.id.tvStock)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        tvFechaRegistro = findViewById(R.id.tvFechaRegistro)
        tvFechaActualizacion = findViewById(R.id.tvFechaActualizacion)
        btnEditar = findViewById(R.id.btnEditar)
        btnCambiarDisponibilidad = findViewById(R.id.btnCambiarDisponibilidad)
        progressBar = findViewById(R.id.progressBar)

        // Establecer BYD como marca por defecto
        tvMarca.text = MARCA_BYD
    }

    private fun setupObservers() {
        // Observar el auto seleccionado
        viewModel.auto.observe(this) { auto ->
            auto?.let {
                mostrarDatosAuto(it)
            } ?: run {
                Toast.makeText(this, "No se pudo cargar la información del auto", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Observar el precio formateado
        viewModel.precio.observe(this) { precioFormateado ->
            tvPrecio.text = precioFormateado
        }

        // Observar fechas formateadas
        viewModel.fechaRegistro.observe(this) { fecha ->
            tvFechaRegistro.text = "Fecha de registro: $fecha"
        }

        viewModel.fechaActualizacion.observe(this) { fecha ->
            tvFechaActualizacion.text = "Última actualización: $fecha"
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar errores
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupActionButtons() {
        // Configurar botón de editar
        btnEditar.setOnClickListener {
            val intent = Intent(this, AgregarAutoActivity::class.java)
            intent.putExtra("AUTO_ID", autoId)
            startActivityForResult(intent, REQUEST_EDIT_AUTO)
        }

        // Configurar botón para cambiar disponibilidad
        btnCambiarDisponibilidad.setOnClickListener {
            val disponibilidadActual = viewModel.auto.value?.disponibilidad ?: false
            val nuevoEstado = !disponibilidadActual

            // Confirmar el cambio
            AlertDialog.Builder(this)
                .setTitle("Cambiar disponibilidad")
                .setMessage("¿Seguro que deseas marcar este auto como ${if (nuevoEstado) "disponible" else "no disponible"}?")
                .setPositiveButton("Sí") { _, _ ->
                    viewModel.actualizarDisponibilidad(autoId, nuevoEstado)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun configurarModoVisualizacion() {
        // Ocultar botones de acción cuando se viene del catálogo
        btnEditar.visibility = View.GONE
        btnCambiarDisponibilidad.visibility = View.GONE
    }

    private fun mostrarDatosAuto(auto: com.example.catalogoautos.model.Auto) {
        // Mostrar imagen del auto (por ahora usamos placeholder)
        ivFotoAuto.setImageResource(R.drawable.ic_launcher_foreground)

        // Mostrar datos del auto
        tvNumeroSerie.text = auto.n_serie
        tvSku.text = auto.sku
        tvModelo.text = auto.modelo
        tvAnio.text = auto.anio.toString()
        tvColor.text = auto.color
        tvStock.text = "${auto.stock} unidades"
        tvDescripcion.text = auto.descripcion

        // Mostrar disponibilidad con estilo visual
        if (auto.disponibilidad) {
            tvDisponibilidad.text = "Disponible"
            tvDisponibilidad.setTextColor(getColor(R.color.green))
        } else {
            tvDisponibilidad.text = "No disponible"
            tvDisponibilidad.setTextColor(getColor(R.color.red))
        }

        // Actualizar el texto del botón de disponibilidad
        btnCambiarDisponibilidad.text = if (auto.disponibilidad)
            "Marcar como No Disponible"
        else
            "Marcar como Disponible"

        // Destacar campos nuevos si corresponde
        if (viewModel.debeMostrarIndicadorNuevoCampo("n_serie")) {
            highlightNewField(tvNumeroSerie)
        }

        if (viewModel.debeMostrarIndicadorNuevoCampo("sku")) {
            highlightNewField(tvSku)
        }
    }

    private fun highlightNewField(textView: TextView) {
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.badge_background, 0)
        textView.compoundDrawablePadding = 8
    }

    // Manejar resultado de la edición del auto
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_AUTO && resultCode == RESULT_OK) {
            // Si el auto fue editado exitosamente, recargarlo
            viewModel.cargarAuto(autoId)
        }
    }

    // Reemplazar onBackPressed() por onBackPressedDispatcher.onBackPressed()
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}