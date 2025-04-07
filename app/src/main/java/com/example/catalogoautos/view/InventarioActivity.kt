package com.example.catalogoautos.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Actividad para gestionar el inventario de autos.
 *
 * Esta actividad muestra la lista de autos disponibles con opciones para agregar,
 * editar y ver detalles de cada auto.
 */
class InventarioActivity : AppCompatActivity(), AutoAdapter.OnAutoClickListener {

    private lateinit var rvAutos: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var autoAdapter: AutoAdapter
    private lateinit var viewModel: InventarioViewModel

    // Agregar una variable para el FloatingActionButton, pero puede ser null
    private var fabAgregar: FloatingActionButton? = null

    // Mapa temporal de marcas - en una implementación real, esto vendría de tu repositorio de marcas
    private val marcasMap = mapOf(
        1 to "Toyota",
        2 to "Honda",
        3 to "Ford",
        4 to "Chevrolet",
        5 to "Nissan"
    )

    // Registro de actividad que se lanza para agregar/editar autos
    private val agregarEditarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // No necesitamos hacer nada específico aquí porque observamos el flujo de autos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        // Configuración de la barra de acción
        supportActionBar?.title = "Inventario de Autos"

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, InventarioViewModel.Factory(this))
            .get(InventarioViewModel::class.java)

        // Configurar vistas
        rvAutos = findViewById(R.id.rvAutos)
        tvEmpty = findViewById(R.id.tvEmpty)

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

        // Configurar RecyclerView
        rvAutos.layoutManager = LinearLayoutManager(this)
        autoAdapter = AutoAdapter(this, marcasMap)
        rvAutos.adapter = autoAdapter

        // Observar cambios en la lista de autos
        lifecycleScope.launch {
            viewModel.autos.collectLatest { autos ->
                autoAdapter.submitList(autos)
                actualizarEstadoVacio(autos)
            }
        }
    }

    // Actualizar visibilidad de la vista vacía según si hay autos o no
    private fun actualizarEstadoVacio(autos: List<Auto>) {
        if (autos.isEmpty()) {
            rvAutos.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvAutos.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
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
}