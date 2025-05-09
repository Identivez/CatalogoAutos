package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var tvTotalAutos: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var cardAgregarAuto: CardView
    private lateinit var cardInventario: CardView
    private lateinit var cardCatalogo: CardView
    private lateinit var cardDetalle: CardView

    private lateinit var viewModel: MenuViewModel
    private val TAG = "MenuActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MenuActivity onCreate iniciado")
        supportActionBar?.hide()
        setContentView(R.layout.activity_menu)

        // Inicializar ViewModel con la Factory actualizada que incluye las dependencias
        viewModel = ViewModelProvider(
            this,
            MenuViewModel.Factory(
                application,
                UsuarioRepository(this),
                AutoRepository(applicationContext)
            )
        ).get(MenuViewModel::class.java)

        // Configurar referencias a vistas
        tvBienvenida = findViewById(R.id.tvBienvenida)
        tvTotalAutos = findViewById(R.id.tvTotalAutos)
        btnLogout = findViewById(R.id.btnLogout)
        cardAgregarAuto = findViewById(R.id.cardAgregarAuto)
        cardInventario = findViewById(R.id.cardInventario)
        cardCatalogo = findViewById(R.id.cardCatalogo)
        cardDetalle = findViewById(R.id.cardDetalle)

        // Observar el usuario actual para mostrar mensaje de bienvenida
        lifecycleScope.launch {
            viewModel.usuarioActual.collect { nombreUsuario ->
                Log.d(TAG, "Usuario recibido: $nombreUsuario")
                if (nombreUsuario == null) {
                    Log.e(TAG, "No hay usuario en sesión, redirigiendo a login")
                    val intent = Intent(this@MenuActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    tvBienvenida.text = "Bienvenido a BYD, $nombreUsuario"
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

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MenuActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MenuActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "MenuActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MenuActivity onDestroy")
    }
}