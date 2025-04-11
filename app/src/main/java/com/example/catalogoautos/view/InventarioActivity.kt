package com.example.catalogoautos.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.InventarioViewModel
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Actividad para gestionar el inventario de autos BYD.
 *
 * Esta actividad muestra la lista de autos disponibles con opciones avanzadas de búsqueda
 * y filtrado. Permite buscar por modelo, número de serie o SKU, además de ofrecer
 * funcionalidades para agregar, editar y visualizar los detalles de cada auto.
 */
class InventarioActivity : AppCompatActivity(), AutoAdapter.OnAutoClickListener {

    // Referencias a elementos del layout
    private lateinit var etBuscar: EditText
    private lateinit var chipGroupFiltros: ChipGroup
    private lateinit var rvAutos: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvContador: TextView
    private lateinit var progressBar: View

    // Adaptador y ViewModel
    private lateinit var autoAdapter: AutoAdapter
    private lateinit var viewModel: InventarioViewModel

    // FloatingActionButton para agregar nuevos autos
    private var fabAgregar: FloatingActionButton? = null

    // Registro de actividad que se lanza para agregar/editar autos
    private val agregarEditarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recargar datos explícitamente para asegurarnos de tener la información más actualizada
            viewModel.cargarAutos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_inventario)

        // Configuración de la barra de acción
        supportActionBar?.title = "Inventario de Autos BYD"

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, InventarioViewModel.Factory(this))
            .get(InventarioViewModel::class.java)

        // Configurar vistas
        setupViews()

        // Configurar RecyclerView y su adaptador
        setupRecyclerView()

        // Configurar listeners para filtros y búsqueda
        setupListeners()

        // Observar cambios en los datos del ViewModel
        observeViewModel()
    }

    private fun setupViews() {
        // Obtener referencias a todas las vistas necesarias
        etBuscar = findViewById(R.id.etBuscar)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)
        rvAutos = findViewById(R.id.rvAutos)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvContador = findViewById(R.id.tvContador)

        // Asegúrate de que esto no sea null
        progressBar = findViewById(R.id.progressBar)

        // Si el progressBar no existe en el layout, crea uno programáticamente
        if (progressBar == null) {
            // Crear un ProgressBar programáticamente
            progressBar = ProgressBar(this).apply {
                id = View.generateViewId()
                visibility = View.GONE
                isIndeterminate = true
            }
            // Añadirlo al layout principal
            val rootView = findViewById<ViewGroup>(android.R.id.content)
            rootView.addView(progressBar, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        // Intentar encontrar el FAB, pero manejar si no existe
        try {
            fabAgregar = findViewById(R.id.fabAgregar)
            fabAgregar?.setOnClickListener {
                val intent = Intent(this, AgregarAutoActivity::class.java)
                agregarEditarLauncher.launch(intent)
            }
        } catch (e: Exception) {
            // El FAB no existe en el layout actual, no hacemos nada
        }
    }
    private fun setupRecyclerView() {
        // Configurar RecyclerView con su layout manager
        rvAutos.layoutManager = LinearLayoutManager(this)

        // Crear el adaptador (ya no necesitamos pasar el mapa de marcas)
        autoAdapter = AutoAdapter(this)

        // Asignar el adaptador al RecyclerView
        rvAutos.adapter = autoAdapter
    }

    private fun setupListeners() {
        // Listener para el campo de búsqueda
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })

        // Listeners para los chips de filtro
        findViewById<View>(R.id.chipModelo).setOnClickListener {
            viewModel.setTipoBusqueda(InventarioViewModel.TipoBusqueda.MODELO)
        }

        findViewById<View>(R.id.chipSerie).setOnClickListener {
            viewModel.setTipoBusqueda(InventarioViewModel.TipoBusqueda.NUMERO_SERIE)
        }

        findViewById<View>(R.id.chipSku).setOnClickListener {
            viewModel.setTipoBusqueda(InventarioViewModel.TipoBusqueda.SKU)
        }

        findViewById<View>(R.id.chipDisponibles).setOnClickListener {
            // Este es un toggle chip, así que verificamos si está seleccionado
            val isChecked = (findViewById<View>(R.id.chipDisponibles) as? com.google.android.material.chip.Chip)?.isChecked ?: false
            viewModel.setSoloDisponibles(isChecked)
        }
    }

    private fun observeViewModel() {
        // Observar cambios en la lista de autos filtrada
        lifecycleScope.launch {
            viewModel.autos.collectLatest { autos ->
                autoAdapter.submitList(autos)
                actualizarEstadoVacio(autos)
                tvContador.text = "Mostrando ${autos.size} autos"
            }
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

        // Observar estadísticas de inventario (opcional)
        lifecycleScope.launch {
            viewModel.estadisticasInventario.collectLatest { stats ->
                val totalAutos = stats["totalAutos"] ?: 0
                val disponibles = stats["autosDisponibles"] ?: 0
                val stockTotal = stats["stockTotal"] ?: 0

                // Actualizar subtítulo de ActionBar con estadísticas
                supportActionBar?.subtitle = "Total: $totalAutos | Disponibles: $disponibles | Stock: $stockTotal"
            }
        }
    }

    // Actualizar visibilidad de la vista vacía según si hay autos o no
    private fun actualizarEstadoVacio(autos: List<Auto>) {
        if (autos.isEmpty()) {
            rvAutos.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvContador.visibility = View.GONE
        } else {
            rvAutos.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            tvContador.visibility = View.VISIBLE
        }
    }

    // Implementación de callback de clic en auto
    override fun onAutoClick(auto: Auto) {
        // Abrir la actividad de detalle del auto
        val intent = Intent(this, DetalleAutoActivity::class.java)
        intent.putExtra("auto_id", auto.auto_id)
        startActivity(intent)
    }

    // Implementación de callback de clic en editar
    override fun onEditClick(auto: Auto) {
        // Abrir la actividad de edición
        val intent = Intent(this, AgregarAutoActivity::class.java)
        intent.putExtra("AUTO_ID", auto.auto_id)
        agregarEditarLauncher.launch(intent)
    }

    // Opcionalmente, podemos implementar un escaneo de código de barras
    fun escanearCodigo() {
        // En una implementación real, aquí iniciarías un scanner de códigos de barras
        // y luego pasarías el resultado a:
        // viewModel.buscarPorCodigoEscaneado(codigoEscaneado)
        Snackbar.make(findViewById(android.R.id.content),
            "Funcionalidad de escaneo próximamente",
            Snackbar.LENGTH_SHORT).show()
    }
}