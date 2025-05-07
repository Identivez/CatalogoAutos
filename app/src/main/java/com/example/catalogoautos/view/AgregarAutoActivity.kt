package com.example.catalogoautos.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.AgregarAutoViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

class AgregarAutoActivity : AppCompatActivity() {

    // Declaración de los elementos de la interfaz
    private lateinit var etNumeroSerie: EditText
    private lateinit var etSku: EditText
    private lateinit var tvMarcaSeleccionada: TextView
    private lateinit var etModelo: EditText
    private lateinit var etAnio: EditText
    private lateinit var etColor: EditText
    private lateinit var etPrecio: EditText
    private lateinit var etStock: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var cbDisponibilidad: CheckBox
    private lateinit var btnSeleccionarFoto: Button
    private lateinit var ivFotoPreview: ImageView
    private lateinit var btnGuardar: Button

    private lateinit var viewModel: AgregarAutoViewModel
    private var selectedImageUri: Uri? = null
    private var autoId: Int? = null

    // Constante para la marca BYD
    private val MARCA_BYD_ID = 1

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val imagePath = copiarImagenAAlmacenamientoInterno(it)
                if (imagePath != null) {
                    selectedImageUri = Uri.fromFile(File(imagePath))
                    ivFotoPreview.setImageURI(selectedImageUri)
                    ivFotoPreview.visibility = View.VISIBLE

                    Log.d("AgregarAutoActivity", "Imagen copiada exitosamente a: $imagePath")
                } else {
                    Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AgregarAutoActivity", "Error al procesar la imagen: ${e.message}", e)
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_agregar_auto)

        // Configurar barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agregar Auto"

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, AgregarAutoViewModel.Factory(this))
            .get(AgregarAutoViewModel::class.java)

        // Obtener el ID del auto a editar si existe
        autoId = intent.getIntExtra("AUTO_ID", -1)
        if (autoId == -1) autoId = null
        viewModel.setAutoParaEditar(autoId)

        // Configurar referencias a vistas
        etNumeroSerie = findViewById(R.id.etNumeroSerie)
        etSku = findViewById(R.id.etSku)
        tvMarcaSeleccionada = findViewById(R.id.tvMarcaSeleccionada)
        etModelo = findViewById(R.id.etModelo)
        etAnio = findViewById(R.id.etAnio)
        etColor = findViewById(R.id.etColor)
        etPrecio = findViewById(R.id.etPrecio)
        etStock = findViewById(R.id.etStock)
        etDescripcion = findViewById(R.id.etDescripcion)
        cbDisponibilidad = findViewById(R.id.cbDisponibilidad)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)
        ivFotoPreview = findViewById(R.id.ivFotoPreview)
        btnGuardar = findViewById(R.id.btnGuardar)

        // Establecer BYD como marca predeterminada
        tvMarcaSeleccionada.text = "BYD"

        // Si estamos editando, llenar los campos con los datos existentes y cambiar el título
        val autoActual = viewModel.getAutoActual()
        if (autoId != null) {
            supportActionBar?.title = "Editar Auto"

            // Llenar campos con los valores del auto a editar
            etNumeroSerie.setText(autoActual.n_serie)
            etSku.setText(autoActual.sku)
            etModelo.setText(autoActual.modelo)
            etAnio.setText(autoActual.anio.toString())
            etColor.setText(autoActual.color)
            etPrecio.setText(autoActual.precio.toString())
            etStock.setText(autoActual.stock.toString())
            etDescripcion.setText(autoActual.descripcion)
            cbDisponibilidad.isChecked = autoActual.disponibilidad
        }

        // Configurar listeners de botones
        btnSeleccionarFoto.setOnClickListener {
            // Usar directamente el enfoque moderno con GetContent
            selectImageLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            guardarAuto()
        }
    }

    // Método para copiar la imagen seleccionada al almacenamiento interno de la aplicación
    private fun copiarImagenAAlmacenamientoInterno(uri: Uri): String? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "auto_image_${System.currentTimeMillis()}.jpg"
            val outputFile = File(filesDir, fileName)

            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("AgregarAutoActivity", "Error copiando imagen: ${e.message}", e)
            return null
        }
    }

    private fun guardarAuto() {
        try {
            // Validar campos obligatorios
            val numeroSerie = etNumeroSerie.text.toString().trim()
            val sku = etSku.text.toString().trim()
            val modelo = etModelo.text.toString().trim()
            val anioStr = etAnio.text.toString().trim()
            val color = etColor.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()
            val stockStr = etStock.text.toString().trim()

            // Validación de campos obligatorios
            if (numeroSerie.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el número de serie", Toast.LENGTH_SHORT).show()
                etNumeroSerie.requestFocus()
                return
            }

            if (sku.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el SKU", Toast.LENGTH_SHORT).show()
                etSku.requestFocus()
                return
            }

            if (sku.length > 10) {
                Toast.makeText(this, "El SKU no puede tener más de 10 caracteres", Toast.LENGTH_SHORT).show()
                etSku.requestFocus()
                return
            }

            if (modelo.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el modelo", Toast.LENGTH_SHORT).show()
                etModelo.requestFocus()
                return
            }

            if (anioStr.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el año", Toast.LENGTH_SHORT).show()
                etAnio.requestFocus()
                return
            }

            if (color.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el color", Toast.LENGTH_SHORT).show()
                etColor.requestFocus()
                return
            }

            if (precioStr.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el precio", Toast.LENGTH_SHORT).show()
                etPrecio.requestFocus()
                return
            }

            if (stockStr.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese el stock", Toast.LENGTH_SHORT).show()
                etStock.requestFocus()
                return
            }

            // Convertir tipos
            val anio = anioStr.toInt()

            // Validar el año según la restricción de la base de datos
            val currentYear = LocalDateTime.now().year
            if (anio <= 1900 || anio > currentYear) {
                Toast.makeText(this, "El año debe estar entre 1901 y $currentYear", Toast.LENGTH_SHORT).show()
                etAnio.requestFocus()
                return
            }

            val precio = precioStr.toDouble()

            // Validar precio según la restricción de la base de datos
            if (precio < 0) {
                Toast.makeText(this, "El precio no puede ser negativo", Toast.LENGTH_SHORT).show()
                etPrecio.requestFocus()
                return
            }

            val stock = stockStr.toInt()

            // Validar stock según la restricción de la base de datos
            if (stock < 0) {
                Toast.makeText(this, "El stock no puede ser negativo", Toast.LENGTH_SHORT).show()
                etStock.requestFocus()
                return
            }

            val descripcion = etDescripcion.text.toString().trim()
            val disponibilidad = cbDisponibilidad.isChecked

            // Fecha actual para actualización
            val fechaActualizacion = LocalDateTime.now()

            // Crear o actualizar el auto
            val auto = if (autoId == null) {
                // Nuevo auto
                Auto(
                    n_serie = numeroSerie,
                    sku = sku,
                    marca_id = MARCA_BYD_ID,
                    modelo = modelo,
                    anio = anio,
                    color = color,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponibilidad = disponibilidad,
                    fecha_registro = LocalDateTime.now(),
                    fecha_actualizacion = fechaActualizacion
                )
            } else {
                // Auto existente
                viewModel.getAutoActual().copy(
                    n_serie = numeroSerie,
                    sku = sku,
                    marca_id = MARCA_BYD_ID,
                    modelo = modelo,
                    anio = anio,
                    color = color,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponibilidad = disponibilidad,
                    fecha_actualizacion = fechaActualizacion
                )
            }

            // Guardar el auto y registrar el resultado
            viewModel.guardarAuto(auto)
            Toast.makeText(
                this,
                if (autoId == null) "Auto agregado correctamente" else "Auto actualizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            // Volver a la pantalla anterior
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Manejar clic en el botón de volver
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
