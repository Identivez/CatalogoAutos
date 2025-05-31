package com.example.catalogoautos.view

import Venta
import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import com.example.catalogoautos.adapter.ReporteAdapter

import com.example.catalogoautos.util.PdfGenerator
import com.example.catalogoautos.viewmodel.ReportesViewModel
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReportesActivity : AppCompatActivity() {

    private lateinit var etFechaInicio: TextInputEditText
    private lateinit var etFechaFin: TextInputEditText
    private lateinit var acEstatus: AutoCompleteTextView
    private lateinit var btnGenerarReporte: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rvReportesRecientes: RecyclerView

    private lateinit var viewModel: ReportesViewModel
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val reportesRecientes = mutableListOf<File>()
    private lateinit var reporteAdapter: ReporteAdapter

    private val calendarInicio = Calendar.getInstance()
    private val calendarFin = Calendar.getInstance()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)


        viewModel = ViewModelProvider(
            this,
            ReportesViewModel.Factory(application)
        )[ReportesViewModel::class.java]


        supportActionBar?.apply {
            title = "Reportes de Ventas"
            setDisplayHomeAsUpEnabled(true)
        }

        initializeViews()


        setupDefaultDates()


        setupAdapters()


        cargarReportesExistentes()


        verificarPermisos()


        viewModel.ventas.observe(this) { ventas ->
            onVentasLoaded(ventas)
        }
    }

    private fun initializeViews() {
        try {
            etFechaInicio = findViewById(R.id.etFechaInicio)
            etFechaFin = findViewById(R.id.etFechaFin)
            acEstatus = findViewById(R.id.acEstatus)
            btnGenerarReporte = findViewById(R.id.btnGenerarReporte)
            progressBar = findViewById(R.id.progressBar)
            rvReportesRecientes = findViewById(R.id.rvReportesRecientes)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar componentes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDefaultDates() {

        calendarFin.time = java.util.Date()
        calendarInicio.time = java.util.Date()
        calendarInicio.add(Calendar.MONTH, -1)

        val today = LocalDateTime.now()
        val monthAgo = today.minusMonths(1)

        etFechaInicio.setText(monthAgo.format(dateFormatter))
        etFechaFin.setText(today.format(dateFormatter))


        etFechaInicio.setOnClickListener { showDatePicker(true) }
        etFechaFin.setOnClickListener { showDatePicker(false) }
    }

    private fun setupAdapters() {

        val estatusOpciones = arrayOf("Todos", "PENDIENTE", "COMPLETADA", "CANCELADA", "ENTREGADA")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estatusOpciones)
        acEstatus.setAdapter(adapter)
        acEstatus.setText(estatusOpciones[0], false)

        reporteAdapter = ReporteAdapter(reportesRecientes, object : ReporteAdapter.OnReporteClickListener {
            override fun onReporteClick(reporte: File) {
                abrirReporte(reporte)
            }
        })
        rvReportesRecientes.layoutManager = LinearLayoutManager(this)
        rvReportesRecientes.adapter = reporteAdapter


        btnGenerarReporte.setOnClickListener { generarReporte() }
    }

    private fun showDatePicker(esInicio: Boolean) {
        val calendar = if (esInicio) calendarInicio else calendarFin
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                if (esInicio) {
                    etFechaInicio.setText(selectedDate.format(dateFormatter))
                } else {
                    etFechaFin.setText(selectedDate.format(dateFormatter))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun generarReporte() {
        val estatus = acEstatus.text.toString()
        val estatusFiltro = if (estatus.equals("Todos", ignoreCase = true)) null else estatus

        progressBar.visibility = View.VISIBLE
        btnGenerarReporte.isEnabled = false


        try {
            val fechaInicio = LocalDateTime.parse(
                etFechaInicio.text.toString() + " 00:00:00",
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )
            val fechaFin = LocalDateTime.parse(
                etFechaFin.text.toString() + " 23:59:59",
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )


            if (fechaInicio.isAfter(fechaFin)) {
                progressBar.visibility = View.GONE
                btnGenerarReporte.isEnabled = true
                Toast.makeText(this, "La fecha inicio no puede ser posterior a la fecha fin", Toast.LENGTH_SHORT).show()
                return
            }


            viewModel.cargarVentas(fechaInicio, fechaFin, estatusFiltro)
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            btnGenerarReporte.isEnabled = true
            Toast.makeText(this, "Error en el formato de fechas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onVentasLoaded(ventas: List<Venta>?) {
        if (ventas.isNullOrEmpty()) {
            progressBar.visibility = View.GONE
            btnGenerarReporte.isEnabled = true
            Toast.makeText(this, "No hay ventas en el periodo seleccionado", Toast.LENGTH_SHORT).show()
            return
        }


        val estatus = acEstatus.text.toString()
        val estatusFiltro = if (estatus.equals("Todos", ignoreCase = true)) null else estatus

        executorService.execute {

            try {
                val fechaInicio = LocalDateTime.parse(
                    etFechaInicio.text.toString() + " 00:00:00",
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                )
                val fechaFin = LocalDateTime.parse(
                    etFechaFin.text.toString() + " 23:59:59",
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                )

                val pdfGenerator = PdfGenerator(this)
                val pdfFile = pdfGenerator.generateVentasReport(
                    ventas,
                    fechaInicio,
                    fechaFin,
                    estatusFiltro
                )

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnGenerarReporte.isEnabled = true

                    if (pdfFile != null) {
                        reportesRecientes.add(0, pdfFile)
                        reporteAdapter.notifyItemInserted(0)
                        rvReportesRecientes.scrollToPosition(0)


                        abrirReporte(pdfFile)

                        Toast.makeText(this, "Reporte generado con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnGenerarReporte.isEnabled = true
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirReporte(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if (viewIntent.resolveActivity(packageManager) != null) {
                startActivity(viewIntent)
            } else {

                val options = arrayOf(
                    "Guardar en Descargas",
                    "Compartir",
                    "Buscar visor de PDF"
                )

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("¿Qué deseas hacer con el reporte?")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> {

                                guardarPdfEnDescargas(file)
                            }
                            1 -> {

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de ventas")
                                    putExtra(Intent.EXTRA_TEXT, "Adjunto reporte de ventas generado desde la aplicación CatálogoAutos.")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
                            }
                            2 -> {

                                val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://search?q=pdf viewer&c=apps")
                                }
                                try {
                                    startActivity(playStoreIntent)
                                } catch (e: Exception) {

                                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://play.google.com/store/search?q=pdf viewer&c=apps")
                                    }
                                    startActivity(webIntent)
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el reporte: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Guarda el archivo PDF en la carpeta de Descargas pública y notifica al usuario
     */
    private fun guardarPdfEnDescargas(pdfFile: File) {
        try {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, pdfFile.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val contentResolver = contentResolver
                val contentUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = contentResolver.insert(contentUri, contentValues)

                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        pdfFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)

                    mostrarMensajeDescargarCompletada(uri)
                } else {
                    Toast.makeText(this, "No se pudo guardar el archivo", Toast.LENGTH_SHORT).show()
                }
            } else {

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, pdfFile.name)

                pdfFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(destFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )

                mostrarMensajeDescargarCompletada(Uri.fromFile(destFile))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra un mensaje con opciones para abrir el archivo descargado
     */
    private fun mostrarMensajeDescargarCompletada(uri: Uri) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Archivo guardado")
            .setMessage("El reporte se ha guardado en la carpeta de Descargas. ¿Qué deseas hacer ahora?")
            .setPositiveButton("Abrir") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_ACTIVITY_NO_HISTORY
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No se encontró una aplicación para abrir PDFs",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNeutralButton("Abrir carpeta") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"),
                            "resource/folder"
                        )
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {

                        val altIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        startActivity(Intent.createChooser(altIntent, "Abrir carpeta de descargas"))
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No se pudo abrir la carpeta de descargas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // Este es el método del Paso 5 - Verifica si se necesitan permisos y los solicita
    private fun verificarPermisos() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
            }
        }
    }

    // Este es el método del Paso 5 - Maneja la respuesta a la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido para guardar archivos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado para guardar archivos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarReportesExistentes() {
        try {
            val docsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
            val files = docsDir?.listFiles { _, name ->
                name.lowercase().startsWith("ventas_") && name.lowercase().endsWith(".pdf")
            }

            reportesRecientes.clear()
            if (!files.isNullOrEmpty()) {
                reportesRecientes.addAll(files)

                reportesRecientes.sortByDescending { it.lastModified() }
            }

            reporteAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar reportes existentes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        executorService.shutdown()
        super.onDestroy()
    }
}