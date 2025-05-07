package com.example.catalogoautos.view


import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.catalogoautos.R
import com.example.catalogoautos.repository.AutoRepository
import com.example.catalogoautos.repository.UsuarioRepository
import com.example.catalogoautos.viewmodel.MenuViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var tvTotalAutos: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var cardAgregarAuto: CardView
    private lateinit var cardInventario: CardView
    private lateinit var cardCatalogo: CardView
    private lateinit var cardDetalle: CardView

    private lateinit var viewModel: MenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_menu)

        // Inicializar ViewModel con la Factory actualizada que incluye las dependencias
        viewModel = ViewModelProvider(this, MenuViewModel.Factory(application, UsuarioRepository(this), AutoRepository(applicationContext)))
            .get(MenuViewModel::class.java)

        // Configurar referencias a vistas
        tvBienvenida = findViewById(R.id.tvBienvenida)
        tvTotalAutos = findViewById(R.id.tvTotalAutos)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        cardAgregarAuto = findViewById(R.id.cardAgregarAuto)
        cardInventario = findViewById(R.id.cardInventario)
        cardCatalogo = findViewById(R.id.cardCatalogo)
        cardDetalle = findViewById(R.id.cardDetalle)

        // Configurar la barra de navegación
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menuHome -> {
                    // Ya estamos en la pantalla principal
                    true
                }
                R.id.menuInventario -> {
                    // Acción para Inventario
                    startActivity(Intent(this, InventarioActivity::class.java))
                    true
                }
                R.id.menuAgregar -> {
                    // Acción para Agregar
                    startActivity(Intent(this, AgregarAutoActivity::class.java))
                    true
                }
                R.id.menuCatalogo -> {
                    // Acción para Catálogo
                    startActivity(Intent(this, CatalogoAutosActivity::class.java))
                    true
                }
                R.id.menuDetalle -> {
                    // Acción para Detalle
                    startActivity(Intent(this, DetalleAutoActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Establecer el item seleccionado por defecto
        bottomNavigation.selectedItemId = R.id.menuHome

        // Observar el usuario actual para mostrar mensaje de bienvenida
        lifecycleScope.launch {
            viewModel.usuarioActual.collect { usuario ->
                if (usuario == null) {
                    // Si no hay usuario logueado, volver a la pantalla de login
                    startActivity(Intent(this@MenuActivity, LoginActivity::class.java))
                    finish()
                } else {
                    tvBienvenida.text = "Bienvenido a BYD, $usuario"
                }
            }
        }

        // Observar el total de autos
        lifecycleScope.launch {
            viewModel.totalAutos.collect { total ->
                tvTotalAutos.text = "$total vehículos"
            }
        }

        // Configurar listeners para los cards
        cardAgregarAuto.setOnClickListener {
            startActivity(Intent(this, AgregarAutoActivity::class.java))
        }

        cardInventario.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
        }

        cardCatalogo.setOnClickListener {
            startActivity(Intent(this, CatalogoAutosActivity::class.java))
        }

        cardDetalle.setOnClickListener {
            startActivity(Intent(this, DetalleAutoActivity::class.java))
        }

        // Listener para el botón de logout
        btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }
}
