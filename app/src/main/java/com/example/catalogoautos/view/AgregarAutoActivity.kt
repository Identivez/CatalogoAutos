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
import android.widget.Spinner
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.AgregarAutoViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class AgregarAutoActivity : AppCompatActivity() {

    private lateinit var spinnerMarca: Spinner
    private lateinit var etModelo: EditText
    private lateinit var etAño: EditText
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
    private var autoActual: Auto? = null


    private val marcas = listOf("Seleccione una marca", "Toyota", "Honda", "Ford", "Chevrolet", "Nissan")
    private val marcasIds = listOf(0, 1, 2, 3, 4, 5)


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
        spinnerMarca = findViewById(R.id.spinnerMarca)
        etModelo = findViewById(R.id.etModelo)
        etAño = findViewById(R.id.etAño)
        etColor = findViewById(R.id.etColor)
        etPrecio = findViewById(R.id.etPrecio)
        etStock = findViewById(R.id.etStock)
        etDescripcion = findViewById(R.id.etDescripcion)
        cbDisponibilidad = findViewById(R.id.cbDisponibilidad)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)
        ivFotoPreview = findViewById(R.id.ivFotoPreview)
        btnGuardar = findViewById(R.id.btnGuardar)

        // Configurar el adaptador para el spinner de marcas
        val marcasAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marcas)
        marcasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarca.adapter = marcasAdapter

        // Si estamos editando, llenar los campos con los datos existentes y cambiar el título
        val autoActual = viewModel.getAutoActual()
        if (autoId != null) {
            supportActionBar?.title = "Editar Auto"

            // Seleccionar la marca correspondiente en el spinner
            val marcaIndex = marcasIds.indexOf(autoActual.marca_id)
            if (marcaIndex >= 0) {
                spinnerMarca.setSelection(marcaIndex)
            }

            etModelo.setText(autoActual.modelo)
            etAño.setText(autoActual.año.toString())
            etColor.setText(autoActual.color)
            etPrecio.setText(autoActual.precio.toString())
            etStock.setText(autoActual.stock.toString())
            etDescripcion.setText(autoActual.descripcion)
            cbDisponibilidad.isChecked = autoActual.disponibilidad

            // Nota: El manejo de fotos ahora debería manejarse a través de la entidad Foto
            // Este código debería actualizarse cuando implementes la gestión de fotos
            // Por ahora, mantenemos la lógica básica para mostrar una visualización de la foto
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
            val marcaPos = spinnerMarca.selectedItemPosition
            if (marcaPos <= 0) {
                Toast.makeText(this, "Por favor, seleccione una marca", Toast.LENGTH_SHORT).show()
                return
            }

            val modelo = etModelo.text.toString().trim()
            val añoStr = etAño.text.toString().trim()
            val color = etColor.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()
            val stockStr = etStock.text.toString().trim()

            if (modelo.isEmpty() || añoStr.isEmpty() || color.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return
            }

            // Convertir tipos
            val marca_id = marcasIds[marcaPos]
            val año = añoStr.toInt()
            val precio = precioStr.toDouble()
            val stock = stockStr.toInt()
            val descripcion = etDescripcion.text.toString().trim()
            val disponibilidad = cbDisponibilidad.isChecked

            // Fecha actual para actualización
            val fechaActualizacion = Date()

            // Registrar información para diagnóstico
            Log.d("AgregarAutoActivity", "Guardando auto: $marca_id - $modelo ($año)")

            // Crear o actualizar el auto
            val auto = if (autoId == null) {
                // Nuevo auto
                Auto(
                    marca_id = marca_id,
                    modelo = modelo,
                    año = año,
                    color = color,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponibilidad = disponibilidad,
                    fecha_registro = Date(),
                    fecha_actualizacion = fechaActualizacion
                )
            } else {
                // Auto existente
                viewModel.getAutoActual().copy(
                    marca_id = marca_id,
                    modelo = modelo,
                    año = año,
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
            Log.d("AgregarAutoActivity", "Auto guardado con ID: ${auto.auto_id}")

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