package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.MenuViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

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

        // Inicializar ViewModel con la Factory actualizada que incluye la aplicación
        viewModel = ViewModelProvider(this, MenuViewModel.Factory(application))
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
        bottomNavigation.setOnNavigationItemSelectedListener(this)
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
            // Si tienes alguna lógica para mostrar detalles de un auto específico, añádela aquí
            // De lo contrario, simplemente abre la actividad de detalles
            startActivity(Intent(this, DetalleAutoActivity::class.java))
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuHome -> {
                // Ya estamos en la pantalla principal
                return true
            }
            R.id.menuInventario -> {
                startActivity(Intent(this, InventarioActivity::class.java))
                return true
            }
            R.id.menuAgregar -> {
                startActivity(Intent(this, AgregarAutoActivity::class.java))
                return true
            }
            R.id.menuCatalogo -> {
                startActivity(Intent(this, CatalogoAutosActivity::class.java))
                return true
            }
            R.id.menuDetalle -> {
                startActivity(Intent(this, DetalleAutoActivity::class.java))
                return true
            }
        }
        return false
    }
}