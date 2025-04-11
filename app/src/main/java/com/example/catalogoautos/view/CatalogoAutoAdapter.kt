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
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Adaptador mejorado para la lista de autos en el catálogo.
 *
 * Este adaptador muestra información detallada sobre cada auto BYD,
 * incluyendo número de serie, SKU, modelo, año, color, precio, stock,
 * descripción y disponibilidad.
 */
class CatalogoAutoAdapter(
    private val onAutoClick: (Auto) -> Unit
) : ListAdapter<Auto, CatalogoAutoAdapter.AutoViewHolder>(AutoDiffCallback()) {

    // Constante para el nombre de la marca
    private val MARCA_BYD = "BYD"

    // Formateador para fechas compatible con LocalDateTime
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalogo_auto, parent, false)
        return AutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AutoViewHolder, position: Int) {
        val auto = getItem(position)
        holder.bind(auto, dateFormatter, onAutoClick, MARCA_BYD)
    }

    class AutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a vistas según el layout actual
        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvAnoEstado: TextView = itemView.findViewById(R.id.tvAnoEstado)
        private val tvColor: TextView = itemView.findViewById(R.id.tvColor)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvKilometraje: TextView = itemView.findViewById(R.id.tvKilometraje)
        private val tvDetallesTecnicos: TextView = itemView.findViewById(R.id.tvDetallesTecnicos)
        private val tvFechaRegistro: TextView = itemView.findViewById(R.id.tvFechaRegistro)

        fun bind(auto: Auto, dateFormatter: DateTimeFormatter, onAutoClick: (Auto) -> Unit, marcaNombre: String) {
            // Configurar información básica
            tvMarcaModelo.text = "$marcaNombre ${auto.modelo}"

            // Mostrar año y estado de disponibilidad
            val estadoDisponibilidad = if (auto.disponibilidad) "Disponible" else "No disponible"
            tvAnoEstado.text = "${auto.anio} • $estadoDisponibilidad"

            // Mostrar el color
            tvColor.text = "Color: ${auto.color}"

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            // Usar el campo stock para simular el kilometraje
            // (ya que el layout tiene kilometraje pero el modelo tiene stock)
            tvKilometraje.text = "${formatoNumeroConSeparadores(auto.stock)} unid."

            // Usar descripción para detalles técnicos
            tvDetallesTecnicos.text = if (auto.descripcion.isNotEmpty()) {
                if (auto.descripcion.length > 20) {
                    auto.descripcion.substring(0, 20) + "..."
                } else {
                    auto.descripcion
                }
            } else {
                "Sin detalles"
            }

            // Formatear la fecha usando LocalDateTime
            tvFechaRegistro.text = auto.fecha_registro.format(dateFormatter)

            // La imagen se manejará a través de la entidad Foto en una implementación completa
            // Por ahora usamos un placeholder
            ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)

            // Configurar clic en todo el item para ver detalles
            itemView.setOnClickListener {
                onAutoClick(auto)
            }

            // Aplicar un estilo visual basado en la disponibilidad
            if (auto.disponibilidad) {
                // Usamos colores estándar en lugar de recursos personalizados
                tvAnoEstado.setTextColor(itemView.context.getResources().getColor(R.color.teal_700, itemView.context.theme))
            } else {
                tvAnoEstado.setTextColor(itemView.context.getResources().getColor(android.R.color.holo_red_light, itemView.context.theme))
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
            // Comparación detallada incluyendo todos los campos relevantes
            return oldItem.auto_id == newItem.auto_id &&
                    oldItem.n_serie == newItem.n_serie &&
                    oldItem.sku == newItem.sku &&
                    oldItem.modelo == newItem.modelo &&
                    oldItem.anio == newItem.anio &&
                    oldItem.color == newItem.color &&
                    oldItem.precio == newItem.precio &&
                    oldItem.stock == newItem.stock &&
                    oldItem.descripcion == newItem.descripcion &&
                    oldItem.disponibilidad == newItem.disponibilidad
        }
    }
}