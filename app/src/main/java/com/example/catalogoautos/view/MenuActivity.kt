package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.catalogoautos.R
import com.example.catalogoautos.viewmodel.MenuViewModel
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var btnAgregarAuto: Button
    private lateinit var btnInventario: Button
    private lateinit var btnCatalogo: Button // Nuevo botón para el catálogo
    private lateinit var btnLogout: Button

    private lateinit var viewModel: MenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, MenuViewModel.Factory())
            .get(MenuViewModel::class.java)

        // Configurar referencias a vistas
        tvBienvenida = findViewById(R.id.tvBienvenida)
        btnAgregarAuto = findViewById(R.id.btnAgregarAuto)
        btnInventario = findViewById(R.id.btnInventario)
        btnCatalogo = findViewById(R.id.btnCatalogo) // Referencia al nuevo botón
        btnLogout = findViewById(R.id.btnLogout)

        // Observar el usuario actual para mostrar mensaje de bienvenida
        lifecycleScope.launch {
            viewModel.usuarioActual.collect { usuario ->
                if (usuario == null) {
                    // Si no hay usuario logueado, volver a la pantalla de login
                    startActivity(Intent(this@MenuActivity, LoginActivity::class.java))
                    finish()
                } else {
                    tvBienvenida.text = "Bienvenido, $usuario"
                }
            }
        }

        // Configurar listeners de botones
        btnAgregarAuto.setOnClickListener {
            startActivity(Intent(this, AgregarAutoActivity::class.java))
        }

        btnInventario.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
        }

        // Listener para el nuevo botón de catálogo
        btnCatalogo.setOnClickListener {
            startActivity(Intent(this, CatalogoAutosActivity::class.java))
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }
}