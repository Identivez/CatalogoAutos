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
 * Este adaptador muestra información más detallada sobre cada auto,
 * incluyendo kilometraje, detalles técnicos y fecha de registro.
 */
class CatalogoAutoAdapter(private val onAutoClick: (Auto) -> Unit) :
    ListAdapter<Auto, CatalogoAutoAdapter.AutoViewHolder>(AutoDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalogo_auto, parent, false)
        return AutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AutoViewHolder, position: Int) {
        val auto = getItem(position)
        holder.bind(auto, dateFormat, onAutoClick)
    }

    class AutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a vistas básicas
        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvAnoEstado: TextView = itemView.findViewById(R.id.tvAnoEstado)
        private val tvColor: TextView = itemView.findViewById(R.id.tvColor)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)

        // Referencias a vistas de detalles adicionales
        private val tvKilometraje: TextView = itemView.findViewById(R.id.tvKilometraje)
        private val tvDetallesTecnicos: TextView = itemView.findViewById(R.id.tvDetallesTecnicos)
        private val tvFechaRegistro: TextView = itemView.findViewById(R.id.tvFechaRegistro)

        fun bind(auto: Auto, dateFormat: SimpleDateFormat, onAutoClick: (Auto) -> Unit) {
            // Configurar información básica
            tvMarcaModelo.text = "${auto.marca} ${auto.modelo}"
            tvAnoEstado.text = "${auto.año} • ${auto.estado}"
            tvColor.text = "Color: ${auto.color}"

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            // Configurar detalles adicionales
            tvKilometraje.text = if (auto.kilometraje > 0)
                "${formatoNumeroConSeparadores(auto.kilometraje)} km"
            else
                "0 km"

            tvDetallesTecnicos.text = if (auto.detallesTecnicos.isNotEmpty())
                auto.detallesTecnicos
            else
                "No disponible"

            tvFechaRegistro.text = dateFormat.format(auto.fechaRegistro)

            // Cargar imagen del auto
            if (auto.fotoPath.isNotEmpty()) {
                try {
                    ivAutoFoto.setImageURI(Uri.parse(auto.fotoPath))
                } catch (e: Exception) {
                    ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

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
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            return oldItem == newItem
        }
    }
}