package com.example.catalogoautos.view

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import com.example.catalogoautos.model.Auto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptador mejorado para la lista de autos en el catálogo.
 *
 * Este adaptador muestra información detallada sobre cada auto,
 * incluyendo marca, modelo, año, color, precio, stock, descripción y disponibilidad.
 */
class CatalogoAutoAdapter(
    private val onAutoClick: (Auto) -> Unit,
    private val marcasMap: Map<Int, String> = emptyMap() // Mapa de IDs de marcas a nombres
) : ListAdapter<Auto, CatalogoAutoAdapter.AutoViewHolder>(AutoDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalogo_auto, parent, false)
        return AutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AutoViewHolder, position: Int) {
        val auto = getItem(position)
        holder.bind(auto, dateFormat, onAutoClick, marcasMap)
    }

    class AutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a vistas básicas
        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvAnoColor: TextView = itemView.findViewById(R.id.tvAnoEstado) // Reusamos este campo
        private val tvColor: TextView = itemView.findViewById(R.id.tvColor)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)

        // Referencias a vistas de detalles adicionales
        private val tvStock: TextView = itemView.findViewById(R.id.tvKilometraje) // Reusamos este campo
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDetallesTecnicos) // Reusamos este campo
        private val tvFechaRegistro: TextView = itemView.findViewById(R.id.tvFechaRegistro)

        fun bind(auto: Auto, dateFormat: SimpleDateFormat, onAutoClick: (Auto) -> Unit, marcasMap: Map<Int, String>) {
            // Obtener el nombre de la marca desde el mapa
            val nombreMarca = marcasMap[auto.marca_id] ?: "Marca ${auto.marca_id}"

            // Configurar información básica
            tvMarcaModelo.text = "$nombreMarca ${auto.modelo}"
            tvAnoColor.text = "${auto.año} • ${if (auto.disponibilidad) "Disponible" else "No disponible"}"
            tvColor.text = "Color: ${auto.color}"

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            // Configurar detalles adicionales
            tvStock.text = "Stock: ${formatoNumeroConSeparadores(auto.stock)}"

            tvDescripcion.text = if (auto.descripcion.isNotEmpty())
                auto.descripcion
            else
                "Sin descripción"

            tvFechaRegistro.text = "Registrado: ${dateFormat.format(auto.fecha_registro)}"

            // La imagen se manejará a través de la entidad Foto en una implementación completa
            // Por ahora usamos un placeholder
            ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)

            // Configurar clic en todo el item para ver detalles
            itemView.setOnClickListener {
                onAutoClick(auto)
            }
        }

        // Función para formatear números con separadores de miles
        private fun formatoNumeroConSeparadores(numero: Int): String {
            return NumberFormat.getNumberInstance(Locale.getDefault()).format(numero)
        }
    }

    class AutoDiffCallback : DiffUtil.ItemCallback<Auto>() {
        override fun areItemsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            return oldItem.auto_id == newItem.auto_id
        }

        override fun areContentsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            return oldItem == newItem
        }
    }

    // Método para actualizar el mapa de marcas si se cargan después
    fun actualizarMarcas(marcas: Map<Int, String>) {
        // Este método sería implementado si necesitas actualizar las marcas después
        // de inicializar el adaptador
    }
}