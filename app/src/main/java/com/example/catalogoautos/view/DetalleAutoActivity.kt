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
import androidx.lifecycle.lifecycleScope
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.DetalleAutoViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetalleAutoActivity : AppCompatActivity() {

    // Elementos de la interfaz de usuario
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
    private lateinit var btnEstadoDisponibilidad: Button // Cambiamos el nombre para que coincida con el ID en el layout
    private lateinit var progressBar: View

    // ViewModel y datos
    private lateinit var viewModel: DetalleAutoViewModel
    private var autoId: Int = -1
    private var fromCatalogo: Boolean = false

    // Constante para la marca BYD
    private val MARCA_BYD = "BYD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
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
        viewModel.cargarAuto(autoId)

        // Observar cambios en los datos
        setupObservers()

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

            // Configurar botón para cambiar disponibilidad
            btnEstadoDisponibilidad.setOnClickListener {
                val disponibilidadActual = viewModel.auto.value?.disponibilidad ?: false
                viewModel.actualizarDisponibilidad(autoId, !disponibilidadActual)
            }
        }
    }

    // En DetalleAutoActivity.kt, actualiza tu método setupViews() así:
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
        // Intentamos con el ID "btnCambiarDisponibilidad" que podría estar en el layout
        btnEstadoDisponibilidad = findViewById(R.id.btnCambiarDisponibilidad)
        progressBar = findViewById(R.id.progressBar)

        // Establecer BYD como marca por defecto
        tvMarca.text = MARCA_BYD
    }
    private fun setupObservers() {
        // Observar cambios en el auto
        viewModel.auto.observe(this) { auto ->
            if (auto != null) {
                mostrarDatosAuto(auto)
            } else {
                Toast.makeText(this, "Error: No se pudo cargar la información del auto", Toast.LENGTH_SHORT).show()
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
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observar errores
        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun configurarModoVisualizacion() {
        // Ocultar botones de edición cuando se viene del catálogo
        btnEditar.visibility = View.GONE
        btnEstadoDisponibilidad.visibility = View.GONE

        // Cambiar el título para indicar modo visualización
        supportActionBar?.title = "Detalles del Auto"
    }

    private fun mostrarDatosAuto(auto: com.example.catalogoautos.model.Auto) {
        // Para la imagen, en una implementación completa buscarías la foto principal
        // usando la relación con la entidad Foto
        // Por ahora mostramos una imagen predeterminada
        ivFotoAuto.setImageResource(R.drawable.ic_launcher_foreground)

        // Mostrar datos del auto incluyendo los nuevos campos
        tvNumeroSerie.text = auto.n_serie
        tvSku.text = auto.sku
        tvModelo.text = auto.modelo
        tvAnio.text = auto.anio.toString()
        tvColor.text = auto.color

        // La disponibilidad y el precio se manejan a través de observadores específicos

        // Mostrar información de stock
        tvStock.text = "Stock: ${formatoNumeroConSeparadores(auto.stock)} unidades"
        tvDescripcion.text = auto.descripcion

        // Actualizar texto del botón de disponibilidad
        btnEstadoDisponibilidad.text = if (auto.disponibilidad)
            "Marcar como No Disponible"
        else
            "Marcar como Disponible"

        // Aplicar estilo visual a la disponibilidad
        if (auto.disponibilidad) {
            tvDisponibilidad.text = "Disponible"
            tvDisponibilidad.setTextColor(getResources().getColor(R.color.green, theme))
        } else {
            tvDisponibilidad.text = "No disponible"
            tvDisponibilidad.setTextColor(getResources().getColor(R.color.red, theme))
        }

        // Destacar campos nuevos si corresponde
        if (viewModel.debeMostrarIndicadorNuevoCampo("n_serie")) {
            highlightNewField(tvNumeroSerie)
        }

        if (viewModel.debeMostrarIndicadorNuevoCampo("sku")) {
            highlightNewField(tvSku)
        }
    }

    // Función para formatear números con separadores de miles
    private fun formatoNumeroConSeparadores(numero: Int): String {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(numero)
    }

    // Función para destacar visualmente los campos nuevos
    private fun highlightNewField(textView: TextView) {
        // Usamos un icono genérico para el indicador "new"
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_launcher_foreground, 0)
        textView.compoundDrawablePadding = 8
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
            viewModel.cargarAuto(autoId)
        }
    }
}