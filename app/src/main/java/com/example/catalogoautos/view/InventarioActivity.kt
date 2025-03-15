package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import com.example.catalogoautos.viewmodel.InventarioViewModel
import kotlinx.coroutines.launch

class InventarioActivity : AppCompatActivity(), AutoAdapter.OnAutoClickListener {

    private lateinit var etBuscar: EditText
    private lateinit var rvAutos: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var viewModel: InventarioViewModel
    private lateinit var adapter: AutoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, InventarioViewModel.Factory(this))
            .get(InventarioViewModel::class.java)

        // Configurar referencias a vistas
        etBuscar = findViewById(R.id.etBuscar)
        rvAutos = findViewById(R.id.rvAutos)
        tvEmpty = findViewById(R.id.tvEmpty)

        // Configurar RecyclerView
        adapter = AutoAdapter(this)
        rvAutos.layoutManager = LinearLayoutManager(this)
        rvAutos.adapter = adapter

        // Observar cambios en la lista de autos
        // Observar cambios en la lista de autos
        lifecycleScope.launch {
            viewModel.autos.collect { autos ->
                Log.d("InventarioActivity", "Recibidos ${autos.size} autos: ${autos.map { it.marca + " " + it.modelo }}")
                adapter.submitList(autos)

                // Mostrar mensaje si no hay autos
                if (autos.isEmpty()) {
                    Log.d("InventarioActivity", "Lista vacía, mostrando mensaje")
                    tvEmpty.visibility = View.VISIBLE
                    rvAutos.visibility = View.GONE
                } else {
                    Log.d("InventarioActivity", "Lista con datos, mostrando RecyclerView")
                    tvEmpty.visibility = View.GONE
                    rvAutos.visibility = View.VISIBLE
                }
            }
        }

        // Configurar búsqueda
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Implementación de la interfaz OnAutoClickListener
    override fun onAutoClick(auto: Auto) {
        // Al hacer clic en un auto, abrir la pantalla de detalles
        val intent = Intent(this, DetalleAutoActivity::class.java)
        intent.putExtra("AUTO_ID", auto.id)
        startActivity(intent)
    }

    override fun onEditClick(auto: Auto) {
        // Al hacer clic en el botón de editar, abrir la pantalla de edición
        val intent = Intent(this, AgregarAutoActivity::class.java)
        intent.putExtra("AUTO_ID", auto.id)
        startActivity(intent)
    }
}