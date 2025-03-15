package com.example.catalogoautos.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.DetalleAutoViewModel
import java.text.NumberFormat
import java.util.Locale
import android.view.MenuItem

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
    private lateinit var btnEditar: Button

    private lateinit var viewModel: DetalleAutoViewModel
    private var autoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_auto)

        // Habilitar botón de volver en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, DetalleAutoViewModel.Factory(this))
            .get(DetalleAutoViewModel::class.java)

        // Obtener ID del auto a mostrar
        autoId = intent.getStringExtra("AUTO_ID")
        if (autoId == null) {
            finish()
            return
        }

        // Configurar referencias a vistas
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

        // Cargar datos del auto
        cargarDatosAuto()

        // Configurar listener del botón de editar
        btnEditar.setOnClickListener {
            val intent = Intent(this, AgregarAutoActivity::class.java)
            intent.putExtra("AUTO_ID", autoId)
            startActivity(intent)
        }
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
            tvKilometraje.text = "${auto.kilometraje} km"
            tvDetallesTecnicos.text = auto.detallesTecnicos
        }
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
        cargarDatosAuto()
    }
}