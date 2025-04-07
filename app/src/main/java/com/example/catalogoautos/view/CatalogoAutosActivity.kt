package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.CatalogoAutosViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Actividad principal para mostrar el catálogo de autos.
 *
 * Esta actividad muestra una lista de autos disponibles con opciones de filtrado.
 * Los usuarios pueden buscar por modelo, filtrar por disponibilidad, marca y establecer un precio máximo.
 */
class CatalogoAutosActivity : AppCompatActivity() {

    // Referencias a elementos del layout
    private lateinit var etBuscar: EditText
    private lateinit var spinnerDisponibilidad: Spinner
    private lateinit var spinnerMarca: Spinner
    private lateinit var etPrecioMaximo: EditText
    private lateinit var btnLimpiarFiltros: Button
    private lateinit var rvAutos: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar

    // ViewModel
    private lateinit var viewModel: CatalogoAutosViewModel

    // Adaptador para la lista de autos
    private lateinit var adapter: CatalogoAutoAdapter

    // Mapa temporal de marcas - en una implementación real, esto vendría de tu repositorio de marcas
    private val marcasMap = mapOf(
        0 to "Todas las marcas",
        1 to "Toyota",
        2 to "Honda",
        3 to "Ford",
        4 to "Chevrolet",
        5 to "Nissan"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo_autos)

        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this, CatalogoAutosViewModel.Factory(this))
            .get(CatalogoAutosViewModel::class.java)

        // Inicializar vistas
        setupViews()

        // Configurar el adaptador y RecyclerView
        setupRecyclerView()

        // Configurar listeners para los filtros
        setupFilterListeners()

        // Observar cambios en los datos
        observeViewModel()
    }

    private fun setupViews() {
        // Obtener referencias a las vistas
        etBuscar = findViewById(R.id.etBuscar)
        spinnerDisponibilidad = findViewById(R.id.spinnerEstado) // Reusamos el spinner de estado
        spinnerMarca = findViewById(R.id.spinnerMarca) // Este spinner debe agregarse al layout
        etPrecioMaximo = findViewById(R.id.etPrecioMaximo)
        btnLimpiarFiltros = findViewById(R.id.btnLimpiarFiltros)
        rvAutos = findViewById(R.id.rvAutos)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)

        // Configurar el Spinner de disponibilidad
        val disponibilidadOptions = arrayOf("Todos", "Disponible", "No disponible")
        val disponibilidadAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, disponibilidadOptions)
        disponibilidadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDisponibilidad.adapter = disponibilidadAdapter

        // Configurar el Spinner de marcas
        val marcasOptions = marcasMap.values.toTypedArray()
        val marcasAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marcasOptions)
        marcasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarca.adapter = marcasAdapter

        // Configurar el botón de limpiar filtros
        btnLimpiarFiltros.setOnClickListener {
            limpiarFiltros()
        }
    }

    private fun setupRecyclerView() {
        // Pasar el mapa de marcas al adaptador
        adapter = CatalogoAutoAdapter(
            onAutoClick = { auto ->
                // Cuando se hace clic en un auto, abrir la pantalla de detalles
                abrirDetalleAuto(auto)
            },
            marcasMap = marcasMap.filterKeys { it > 0 } // Excluimos la opción "Todas las marcas"
        )

        rvAutos.layoutManager = LinearLayoutManager(this)
        rvAutos.adapter = adapter
    }

    private fun setupFilterListeners() {
        // Listener para el campo de búsqueda
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })

        // Listener para el spinner de disponibilidad
        spinnerDisponibilidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val disponibilidad = when (position) {
                    0 -> null // Todos
                    1 -> true // Disponible
                    2 -> false // No disponible
                    else -> null
                }
                viewModel.setDisponibilidadFiltro(disponibilidad)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener para el spinner de marcas
        spinnerMarca.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val marcaId = if (position == 0) null else position
                viewModel.setMarcaFiltro(marcaId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener para el precio máximo
        etPrecioMaximo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val precioStr = s.toString()
                viewModel.setPrecioMaximo(if (precioStr.isNotEmpty()) precioStr.toDoubleOrNull() else null)
            }
        })
    }

    private fun observeViewModel() {
        // Observar cambios en la lista de autos
        lifecycleScope.launch {
            viewModel.autos.collectLatest { autos ->
                adapter.submitList(autos)
                updateEmptyState(autos)
            }
        }

        // Observar el estado de carga
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observar errores
        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                if (error != null) {
                    // Aquí podrías mostrar un mensaje de error, por ejemplo con un Snackbar
                    // Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateEmptyState(autos: List<Auto>) {
        if (autos.isEmpty()) {
            rvAutos.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvAutos.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun limpiarFiltros() {
        etBuscar.setText("")
        spinnerDisponibilidad.setSelection(0) // "Todos"
        spinnerMarca.setSelection(0) // "Todas las marcas"
        etPrecioMaximo.setText("")
        viewModel.limpiarFiltros()
    }

    private fun abrirDetalleAuto(auto: Auto) {
        // Abrir la actividad de detalle del auto
        val intent = Intent(this, DetalleAutoActivity::class.java).apply {
            putExtra("auto_id", auto.auto_id) // Ahora usamos auto_id que es Int
            // Indicamos que viene del catálogo
            putExtra("from_catalogo", true)
        }
        startActivity(intent)
    }
}