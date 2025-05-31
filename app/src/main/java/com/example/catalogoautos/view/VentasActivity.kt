
package com.example.catalogoautos.view

import Venta
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catalogoautos.R
import com.example.catalogoautos.view.VentasAdapter
import com.example.catalogoautos.databinding.ActivityVentasBinding

import com.example.catalogoautos.viewmodel.VentasViewModel

class VentasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVentasBinding
    private lateinit var viewModel: VentasViewModel
    private lateinit var adaptador: VentasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVentasBinding.inflate(layoutInflater)
        setContentView(binding.root)


        supportActionBar?.hide()


        viewModel = ViewModelProvider(this, VentasViewModel.Factory(application))[VentasViewModel::class.java]


        setupRecyclerView()


        setupFilters()


        binding.fabAgregarVenta.setOnClickListener {
            val intent = Intent(this, RegistrarVentaActivity::class.java)
            startActivity(intent)
        }


        observeViewModel()


        viewModel.obtenerTodasLasVentas()
    }

    private fun setupRecyclerView() {
        adaptador = VentasAdapter { venta ->

            val intent = Intent(this, DetalleVentaActivity::class.java)
            intent.putExtra("VENTA_ID", venta.ventaId)
            startActivity(intent)
        }

        binding.recyclerViewVentas.apply {
            layoutManager = LinearLayoutManager(this@VentasActivity)
            adapter = adaptador
        }
    }

    private fun setupFilters() {
        binding.spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.btnFiltrar.setOnClickListener {
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        when (binding.spinnerFiltro.selectedItemPosition) {
            0 -> viewModel.obtenerTodasLasVentas() // Todas
            1 -> viewModel.obtenerVentasPorEstatus("COMPLETADA") // Completadas
            2 -> viewModel.obtenerVentasPorEstatus("PENDIENTE") // Pendientes
            3 -> viewModel.obtenerVentasPorEstatus("ENTREGADA") // Entregadas
            4 -> viewModel.obtenerVentasPorEstatus("CANCELADA") // Canceladas
        }
    }

    private fun observeViewModel() {

        viewModel.ventas.observe(this) { ventas ->
            if (ventas != null) {
                adaptador.actualizarVentas(ventas)
                mostrarEstadoVacio(ventas.isEmpty())
                actualizarEstadisticas(ventas)
            }
        }


        viewModel.isLoading.observe(this) { estaCargando ->
            binding.progressBar.visibility = if (estaCargando) View.VISIBLE else View.GONE
        }


        viewModel.error.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }


        viewModel.success.observe(this) { mensaje ->
            if (!mensaje.isNullOrEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarEstadoVacio(mostrar: Boolean) {
        binding.tvEmptyState.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.recyclerViewVentas.visibility = if (mostrar) View.GONE else View.VISIBLE
    }

    private fun actualizarEstadisticas(ventas: List<Venta>) {

        val totalVentas = ventas.size
        val ventasCompletadas = ventas.count { it.estatus == "COMPLETADA" || it.estatus == "ENTREGADA" }
        val ventasPendientes = ventas.count { it.estatus == "PENDIENTE" }

        binding.tvTotalVentas.text = totalVentas.toString()
        binding.tvVentasCompletadas.text = ventasCompletadas.toString()
        binding.tvVentasPendientes.text = ventasPendientes.toString()
    }

    override fun onResume() {
        super.onResume()

        viewModel.obtenerTodasLasVentas()
    }
}