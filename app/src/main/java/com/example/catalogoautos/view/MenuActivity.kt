package com.example.catalogoautos.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
    private var cardVentas: CardView? = null

    private lateinit var viewModel: MenuViewModel
    private val TAG = "MenuActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MenuActivity onCreate iniciado")

        try {
            supportActionBar?.hide()
            setContentView(R.layout.activity_menu)


            viewModel = ViewModelProvider(
                this,
                MenuViewModel.Factory(
                    application,
                    UsuarioRepository(this),
                    AutoRepository(applicationContext)
                )
            ).get(MenuViewModel::class.java)


            initializeViews()


            setupObservers()


            setupClickListeners()

            Log.d(TAG, "MenuActivity onCreate completado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar el menú principal: ${e.message}", Toast.LENGTH_LONG).show()


            try {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                Log.e(TAG, "Error adicional al intentar volver a login: ${ex.message}", ex)
                finish() // Si todo falla, simplemente cerramos
            }
        }
    }

    private fun initializeViews() {
        try {
            tvBienvenida = findViewById(R.id.tvBienvenida)
            tvTotalAutos = findViewById(R.id.tvTotalAutos)
            btnLogout = findViewById(R.id.btnLogout)
            cardAgregarAuto = findViewById(R.id.cardAgregarAuto)
            cardInventario = findViewById(R.id.cardInventario)
            cardCatalogo = findViewById(R.id.cardCatalogo)


            try {
                cardVentas = findViewById(R.id.cardVentas)
                Log.d(TAG, "Card de ventas inicializada correctamente")
            } catch (e: Exception) {
                Log.w(TAG, "La card de ventas no existe en el layout: ${e.message}")

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
            throw e
        }
    }

    private fun setupObservers() {

        lifecycleScope.launch {
            try {
                viewModel.usuarioActual.collect { nombreUsuario ->
                    Log.d(TAG, "Usuario recibido: $nombreUsuario")
                    if (nombreUsuario == null) {
                        Log.e(TAG, "No hay usuario en sesión, redirigiendo a login")

                        try {
                            val intent = Intent(this@MenuActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al redirigir a login: ${e.message}", e)
                            finish()
                        }
                    } else {
                        try {
                            tvBienvenida.text = "Bienvenido a BYD, $nombreUsuario"
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al actualizar mensaje de bienvenida: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al observar usuario actual: ${e.message}", e)
            }
        }


        lifecycleScope.launch {
            try {
                viewModel.totalAutos.collect { total ->
                    try {
                        tvTotalAutos.text = "$total vehículos"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al actualizar total de autos: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al observar total de autos: ${e.message}", e)
            }
        }
    }

    private fun setupClickListeners() {
        try {

            cardAgregarAuto.setOnClickListener {
                try {
                    Log.d(TAG, "Iniciando AgregarAutoActivity")
                    startActivity(Intent(this, AgregarAutoActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar AgregarAutoActivity: ${e.message}", e)
                    Toast.makeText(this, "Error al abrir pantalla de agregar auto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            cardInventario.setOnClickListener {
                try {
                    Log.d(TAG, "Iniciando InventarioActivity")
                    startActivity(Intent(this, InventarioActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar InventarioActivity: ${e.message}", e)
                    Toast.makeText(this, "Error al abrir inventario: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            cardCatalogo.setOnClickListener {
                try {
                    Log.d(TAG, "Iniciando CatalogoAutosActivity")
                    startActivity(Intent(this, CatalogoAutosActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar CatalogoAutosActivity: ${e.message}", e)
                    Toast.makeText(this, "Error al abrir catálogo: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }


            cardVentas?.setOnClickListener {
                showVentasOptions()
            }


            btnLogout.setOnClickListener {
                try {
                    Log.d(TAG, "Cerrando sesión")
                    viewModel.logout()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al cerrar sesión: ${e.message}", e)
                    Toast.makeText(this, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_SHORT).show()


                    try {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error adicional al intentar volver a login: ${ex.message}", ex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
            throw e
        }
    }

    /**
     * Muestra un diálogo con opciones para ir a la pantalla de ventas o de reportes
     */
    private fun showVentasOptions() {
        try {
            val options = arrayOf("Gestión de ventas", "Reportes de ventas")

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Seleccione una opción")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> {

                            try {
                                Log.d(TAG, "Iniciando VentasActivity")
                                startActivity(Intent(this, VentasActivity::class.java))
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al iniciar VentasActivity: ${e.message}", e)
                                Toast.makeText(this, "Error al abrir gestión de ventas: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {

                            try {
                                Log.d(TAG, "Iniciando ReportesActivity")
                                startActivity(Intent(this, ReportesActivity::class.java))
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al iniciar ReportesActivity: ${e.message}", e)
                                Toast.makeText(this, "Error al abrir reportes: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar opciones de ventas: ${e.message}", e)


            try {
                startActivity(Intent(this, VentasActivity::class.java))
            } catch (ex: Exception) {
                Log.e(TAG, "Error al iniciar VentasActivity como fallback: ${ex.message}", ex)
                Toast.makeText(this, "Error al acceder a las funciones de ventas", Toast.LENGTH_SHORT).show()
            }
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