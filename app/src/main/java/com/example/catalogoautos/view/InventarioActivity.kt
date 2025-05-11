package com.example.catalogoautos.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.repository.AutoRepository
import com.example.catalogoautos.viewmodel.InventarioViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventarioActivity : AppCompatActivity(), AutoAdapter.OnAutoClickListener {

    // Referencias a las vistas
    private lateinit var etBuscar: EditText
    private lateinit var chipGroupFiltros: ChipGroup
    private lateinit var rvAutos: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvContador: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabAgregar: FloatingActionButton

    // ViewModel y Adapter
    private lateinit var viewModel: InventarioViewModel
    private lateinit var adapter: AutoAdapter

    // Launcher para agregar/editar autos
    private val agregarEditarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recargar datos cuando regresamos de agregar/editar
            viewModel.cargarAutos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        // Configurar título
        supportActionBar?.title = "Inventario de Autos BYD"

        // Inicializar ViewModel
        viewModel = ViewModelProvider(
            this,
            InventarioViewModel.Factory(AutoRepository.getInstance(applicationContext))
        ).get(InventarioViewModel::class.java)

        // Inicializar vistas
        setupViews()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar listeners
        setupListeners()

        // Observar cambios en el ViewModel
        observeViewModel()

        // Cargar datos
        viewModel.cargarAutos()
    }

    private fun setupViews() {
        etBuscar = findViewById(R.id.etBuscar)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)
        rvAutos = findViewById(R.id.rvAutos)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvContador = findViewById(R.id.tvContador)
        progressBar = findViewById(R.id.progressBar)

        // SwipeRefreshLayout para recargar datos al deslizar
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // FloatingActionButton para agregar
        fabAgregar = findViewById(R.id.fabAgregar)
    }

    private fun setupRecyclerView() {
        adapter = AutoAdapter(this)
        rvAutos.layoutManager = LinearLayoutManager(this)
        rvAutos.adapter = adapter
    }

    private fun setupListeners() {
        // Buscar al escribir
        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })

        // Chips de filtro
        findViewById<Chip>(R.id.chipModelo).setOnClickListener {
            viewModel.setTipoBusqueda(InventarioViewModel.TipoBusqueda.MODELO)
        }

        findViewById<Chip>(R.id.chipSerie).setOnClickListener {
            viewModel.setTipoBusqueda(InventarioViewModel.TipoBusqueda.NUMERO_SERIE)
        }

        findViewById<Chip>(R.id.chipSku).setOnClickListener {
            viewModel.setTipoBusqueda(InventarioViewModel.TipoBusqueda.SKU)
        }

        findViewById<Chip>(R.id.chipDisponibles).setOnClickListener {
            val isChecked = (it as Chip).isChecked
            viewModel.setSoloDisponibles(isChecked)
        }

        // SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.cargarAutos()
        }

        // FloatingActionButton
        fabAgregar.setOnClickListener {
            val intent = Intent(this, AgregarAutoActivity::class.java)
            agregarEditarLauncher.launch(intent)
        }
    }

    private fun observeViewModel() {
        // Observar lista de autos
        lifecycleScope.launch {
            viewModel.autos.collectLatest { autos ->
                adapter.submitList(autos)
                actualizarEstadoVacio(autos)
                tvContador.text = "Mostrando ${autos.size} autos"
            }
        }

        // Observar estado de carga
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                swipeRefreshLayout.isRefreshing = isLoading
            }
        }

        // Observar errores
        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@InventarioActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observar estadísticas
        lifecycleScope.launch {
            viewModel.estadisticasInventario.collectLatest { stats ->
                val totalAutos = stats["totalAutos"] ?: 0
                val autosDisponibles = stats["autosDisponibles"] ?: 0
                val stockTotal = stats["stockTotal"] ?: 0

                supportActionBar?.subtitle = "Total: $totalAutos | Disponibles: $autosDisponibles | Stock: $stockTotal"
            }
        }
    }

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

    // Implementación de callbacks de AutoAdapter.OnAutoClickListener
    override fun onAutoClick(auto: Auto) {
        val intent = Intent(this, DetalleAutoActivity::class.java)
        intent.putExtra("auto_id", auto.auto_id)
        startActivity(intent)
    }

    override fun onEditClick(auto: Auto) {
        val intent = Intent(this, AgregarAutoActivity::class.java)
        intent.putExtra("AUTO_ID", auto.auto_id)
        agregarEditarLauncher.launch(intent)
    }
}