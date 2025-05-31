package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.CatalogoAutosViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Actividad principal para mostrar el catálogo de autos BYD.
 *
 * Esta actividad muestra una lista de autos disponibles con opciones de filtrado avanzado.
 * Los usuarios pueden buscar por modelo, número de serie o SKU, filtrar por disponibilidad,
 * y establecer rangos de precio y año.
 */
class CatalogoAutosActivity : AppCompatActivity() {


    private lateinit var etBuscar: EditText
    private lateinit var chipGroupTipoBusqueda: ChipGroup
    private lateinit var chipModelo: Chip
    private lateinit var chipSerie: Chip
    private lateinit var chipSku: Chip
    private lateinit var rvAutos: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLimpiarFiltros: Button


    private lateinit var viewModel: CatalogoAutosViewModel


    private lateinit var adapter: CatalogoAutoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_catalogo_autos)


        viewModel = ViewModelProvider(this, CatalogoAutosViewModel.Factory(this))
            .get(CatalogoAutosViewModel::class.java)


        setupViews()

        setupRecyclerView()


        setupFilterListeners()


        observeViewModel()
    }

    private fun setupViews() {

        etBuscar = findViewById(R.id.etBuscar)
        chipGroupTipoBusqueda = findViewById(R.id.chipGroupFiltros)
        chipModelo = findViewById(R.id.chipModelo)
        chipSerie = findViewById(R.id.chipSerie)
        chipSku = findViewById(R.id.chipSku)
        rvAutos = findViewById(R.id.rvAutos)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        btnLimpiarFiltros = findViewById(R.id.btnLimpiarFiltros)


        btnLimpiarFiltros.setOnClickListener {
            limpiarFiltros()
        }


        chipModelo.isChecked = true
    }

    private fun setupRecyclerView() {

        adapter = CatalogoAutoAdapter(
            onAutoClick = { auto ->

                abrirDetalleAuto(auto)
            }
        )

        rvAutos.layoutManager = LinearLayoutManager(this)
        rvAutos.adapter = adapter
    }

    private fun setupFilterListeners() {

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })


        chipModelo.setOnClickListener {
            viewModel.setTipoBusqueda(CatalogoAutosViewModel.TipoBusqueda.MODELO)
        }

        chipSerie.setOnClickListener {
            viewModel.setTipoBusqueda(CatalogoAutosViewModel.TipoBusqueda.NUMERO_SERIE)
        }

        chipSku.setOnClickListener {
            viewModel.setTipoBusqueda(CatalogoAutosViewModel.TipoBusqueda.SKU)
        }
    }

    private fun observeViewModel() {

        lifecycleScope.launch {
            viewModel.autos.collectLatest { autos ->
                adapter.submitList(autos)
                updateEmptyState(autos)
            }
        }


        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }


        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                if (error != null) {
                    Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG).show()
                }
            }
        }


        lifecycleScope.launch {
            viewModel.estadisticasCatalogo.collectLatest { stats ->
                val totalAutos = stats["totalAutos"] ?: 0
                val autosDisponibles = stats["autosDisponibles"] ?: 0
                val stockTotal = stats["stockTotal"] ?: 0

                supportActionBar?.subtitle = "Total: $totalAutos | Disponibles: $autosDisponibles | Stock: $stockTotal"
            }
        }


        lifecycleScope.launch {
            viewModel.hayFiltrosActivos.collectLatest { hayFiltros ->
                btnLimpiarFiltros.visibility = if (hayFiltros) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateEmptyState(autos: List<Any?>) {

        val autosList = autos.filterIsInstance<Auto>()

        if (autosList.isEmpty()) {
            rvAutos.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvAutos.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun limpiarFiltros() {
        etBuscar.setText("")
        chipModelo.isChecked = true
        viewModel.limpiarFiltros()
    }

    private fun abrirDetalleAuto(auto: Auto) {
      val intent = Intent(this, DetalleAutoActivity::class.java).apply {
            putExtra("auto_id", auto.auto_id)

            putExtra("from_catalogo", true)
        }
        startActivity(intent)
    }
}