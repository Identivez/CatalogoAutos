package com.example.catalogoautos.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.AgregarAutoViewModel
import java.io.File
import java.io.FileOutputStream

class AgregarAutoActivity : AppCompatActivity() {

    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var etAño: EditText
    private lateinit var etColor: EditText
    private lateinit var etPrecio: EditText
    private lateinit var rgEstado: RadioGroup
    private lateinit var rbNuevo: RadioButton
    private lateinit var rbUsado: RadioButton
    private lateinit var etKilometraje: EditText
    private lateinit var etDetallesTecnicos: EditText
    private lateinit var btnSeleccionarFoto: Button
    private lateinit var ivFotoPreview: ImageView
    private lateinit var btnGuardar: Button

    private lateinit var viewModel: AgregarAutoViewModel
    private var selectedImageUri: Uri? = null
    private var autoId: String? = null
    private var autoActual: Auto? = null

    // Usar GetContent para seleccionar imágenes (maneja permisos internamente)
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Copiar la imagen seleccionada al almacenamiento interno
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
        setContentView(R.layout.activity_agregar_auto)

        // Configurar barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agregar Auto"

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, AgregarAutoViewModel.Factory(this))
            .get(AgregarAutoViewModel::class.java)

        // Obtener el ID del auto a editar si existe
        autoId = intent.getStringExtra("AUTO_ID")
        viewModel.setAutoParaEditar(autoId)

        // Configurar referencias a vistas
        etMarca = findViewById(R.id.etMarca)
        etModelo = findViewById(R.id.etModelo)
        etAño = findViewById(R.id.etAño)
        etColor = findViewById(R.id.etColor)
        etPrecio = findViewById(R.id.etPrecio)
        rgEstado = findViewById(R.id.rgEstado)
        rbNuevo = findViewById(R.id.rbNuevo)
        rbUsado = findViewById(R.id.rbUsado)
        etKilometraje = findViewById(R.id.etKilometraje)
        etDetallesTecnicos = findViewById(R.id.etDetallesTecnicos)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)
        ivFotoPreview = findViewById(R.id.ivFotoPreview)
        btnGuardar = findViewById(R.id.btnGuardar)

        // Si estamos editando, llenar los campos con los datos existentes y cambiar el título
        val autoActual = viewModel.getAutoActual()
        if (autoId != null) {
            supportActionBar?.title = "Editar Auto"

            etMarca.setText(autoActual.marca)
            etModelo.setText(autoActual.modelo)
            etAño.setText(autoActual.año.toString())
            etColor.setText(autoActual.color)
            etPrecio.setText(autoActual.precio.toString())

            if (autoActual.estado == "Usado") {
                rbUsado.isChecked = true
            } else {
                rbNuevo.isChecked = true
            }

            etKilometraje.setText(autoActual.kilometraje.toString())
            etDetallesTecnicos.setText(autoActual.detallesTecnicos)

            // Manejo mejorado de la carga de imágenes para prevenir errores
            if (autoActual.fotoPath.isNotEmpty()) {
                try {
                    val file = File(autoActual.fotoPath)
                    if (file.exists()) {
                        // Si es una ruta de archivo local
                        selectedImageUri = Uri.fromFile(file)
                    } else {
                        // Si es una URI de contenido
                        selectedImageUri = Uri.parse(autoActual.fotoPath)
                    }

                    ivFotoPreview.setImageURI(selectedImageUri)
                    ivFotoPreview.visibility = View.VISIBLE
                    Log.d("AgregarAutoActivity", "Cargando imagen desde: ${autoActual.fotoPath}")
                } catch (e: Exception) {
                    Log.e("AgregarAutoActivity", "Error al cargar imagen: ${e.message}", e)
                    // No mostrar la imagen si hay un error
                    selectedImageUri = null
                }
            }
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
            val marca = etMarca.text.toString().trim()
            val modelo = etModelo.text.toString().trim()
            val añoStr = etAño.text.toString().trim()
            val color = etColor.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()

            if (marca.isEmpty() || modelo.isEmpty() || añoStr.isEmpty() || color.isEmpty() || precioStr.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return
            }

            // Convertir tipos
            val año = añoStr.toInt()
            val precio = precioStr.toDouble()
            val estado = if (rbNuevo.isChecked) "Nuevo" else "Usado"
            val kilometraje = etKilometraje.text.toString().trim().let {
                if (it.isEmpty()) 0 else it.toInt()
            }
            val detallesTecnicos = etDetallesTecnicos.text.toString().trim()
            val fotoPath = selectedImageUri?.toString() ?: ""

            // Registrar información para diagnóstico
            Log.d("AgregarAutoActivity", "Guardando auto: $marca $modelo ($año)")

            // Crear o actualizar el auto
            val auto = viewModel.getAutoActual().copy(
                marca = marca,
                modelo = modelo,
                año = año,
                color = color,
                precio = precio,
                estado = estado,
                kilometraje = kilometraje,
                detallesTecnicos = detallesTecnicos,
                fotoPath = fotoPath
            )

            // Guardar el auto y registrar el resultado
            viewModel.guardarAuto(auto)
            Log.d("AgregarAutoActivity", "Auto guardado con ID: ${auto.id}")

            Toast.makeText(
                this,
                if (autoId == null) "Auto agregado correctamente" else "Auto actualizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            // Ahora que el auto se ha guardado correctamente, regresamos a la pantalla anterior
            // Configurar un resultado antes de finalizar para refrescar la lista de autos
            val resultIntent = Intent()
            resultIntent.putExtra("AUTO_GUARDADO", true)
            setResult(Activity.RESULT_OK, resultIntent)

            finish()
        } catch (e: Exception) {
            // Registrar el error para diagnóstico
            Log.e("AgregarAutoActivity", "Error al guardar auto: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Manejar clic en el botón de volver
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}