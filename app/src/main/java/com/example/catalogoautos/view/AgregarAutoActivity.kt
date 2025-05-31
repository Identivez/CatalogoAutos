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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.AgregarAutoViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

class AgregarAutoActivity : AppCompatActivity() {


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
    private lateinit var progressBar: ProgressBar

    private lateinit var viewModel: AgregarAutoViewModel
    private var selectedImageUri: Uri? = null
    private var autoId: Int? = null


    private val MARCA_BYD_ID = 1
    private val TAG = "AgregarAutoActivity"

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

                    Log.d(TAG, "Imagen copiada exitosamente a: $imagePath")
                } else {
                    Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar la imagen: ${e.message}", e)
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_agregar_auto)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agregar Auto"


        viewModel = ViewModelProvider(this, AgregarAutoViewModel.Factory(this))
            .get(AgregarAutoViewModel::class.java)


        autoId = intent.getIntExtra("AUTO_ID", -1)
        if (autoId == -1) autoId = null
        viewModel.setAutoParaEditar(autoId)


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
        progressBar = findViewById(R.id.progressBar)


        tvMarcaSeleccionada.text = "BYD"


        val autoActual = viewModel.getAutoActual()
        if (autoId != null) {
            supportActionBar?.title = "Editar Auto"


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


        btnSeleccionarFoto.setOnClickListener {

            selectImageLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            guardarAuto()
        }


        setupObservers()
    }

    private fun setupObservers() {

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnGuardar.isEnabled = !isLoading
        }


        viewModel.errorMessage.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }


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
            Log.e(TAG, "Error copiando imagen: ${e.message}", e)
            return null
        }
    }

    private fun guardarAuto() {
        try {

            val numeroSerie = etNumeroSerie.text.toString().trim()
            val sku = etSku.text.toString().trim()
            val modelo = etModelo.text.toString().trim()
            val anioStr = etAnio.text.toString().trim()
            val color = etColor.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()
            val stockStr = etStock.text.toString().trim()


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

            val anio = anioStr.toInt()


            val currentYear = LocalDateTime.now().year
            if (anio <= 1900 || anio > currentYear) {
                Toast.makeText(this, "El año debe estar entre 1901 y $currentYear", Toast.LENGTH_SHORT).show()
                etAnio.requestFocus()
                return
            }

            val precio = precioStr.toDouble()


            if (precio < 0) {
                Toast.makeText(this, "El precio no puede ser negativo", Toast.LENGTH_SHORT).show()
                etPrecio.requestFocus()
                return
            }

            val stock = stockStr.toInt()


            if (stock < 0) {
                Toast.makeText(this, "El stock no puede ser negativo", Toast.LENGTH_SHORT).show()
                etStock.requestFocus()
                return
            }

            val descripcion = etDescripcion.text.toString().trim()
            val disponibilidad = cbDisponibilidad.isChecked


            val fechaActualizacion = LocalDateTime.now()


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

            progressBar.visibility = View.VISIBLE
            btnGuardar.isEnabled = false


            lifecycleScope.launch {
                try {

                    val resultado = viewModel.guardarAutoRemoto(auto)

                    resultado.fold(
                        onSuccess = {
                            Toast.makeText(
                                this@AgregarAutoActivity,
                                if (autoId == null) "Auto agregado correctamente al servidor"
                                else "Auto actualizado correctamente en el servidor",
                                Toast.LENGTH_SHORT
                            ).show()


                            setResult(RESULT_OK)
                            finish()
                        },
                        onFailure = { error ->

                            Toast.makeText(
                                this@AgregarAutoActivity,
                                "Auto guardado localmente pero hubo un error en el servidor: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            setResult(RESULT_OK)
                            finish()
                        }
                    )
                } catch (e: Exception) {

                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        btnGuardar.isEnabled = true
                        Toast.makeText(
                            this@AgregarAutoActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}