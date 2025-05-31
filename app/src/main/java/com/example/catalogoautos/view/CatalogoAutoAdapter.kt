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


    private val MARCA_BYD = "BYD"


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

        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvAnoEstado: TextView = itemView.findViewById(R.id.tvAnoEstado)
        private val tvColor: TextView = itemView.findViewById(R.id.tvColor)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvKilometraje: TextView = itemView.findViewById(R.id.tvKilometraje)
        private val tvDetallesTecnicos: TextView = itemView.findViewById(R.id.tvDetallesTecnicos)
        private val tvFechaRegistro: TextView = itemView.findViewById(R.id.tvFechaRegistro)

        fun bind(auto: Auto, dateFormatter: DateTimeFormatter, onAutoClick: (Auto) -> Unit, marcaNombre: String) {

            tvMarcaModelo.text = "$marcaNombre ${auto.modelo}"


            val estadoDisponibilidad = if (auto.disponibilidad) "Disponible" else "No disponible"
            tvAnoEstado.text = "${auto.anio} • $estadoDisponibilidad"


            tvColor.text = "Color: ${auto.color}"


            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)


            tvKilometraje.text = "${formatoNumeroConSeparadores(auto.stock)} unid."


            tvDetallesTecnicos.text = if (auto.descripcion.isNotEmpty()) {
                if (auto.descripcion.length > 20) {
                    auto.descripcion.substring(0, 20) + "..."
                } else {
                    auto.descripcion
                }
            } else {
                "Sin detalles"
            }


            tvFechaRegistro.text = auto.fecha_registro.format(dateFormatter)


            ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)


            itemView.setOnClickListener {
                onAutoClick(auto)
            }


            if (auto.disponibilidad) {
                // Usamos colores estándar en lugar de recursos personalizados
                tvAnoEstado.setTextColor(itemView.context.getResources().getColor(R.color.teal_700, itemView.context.theme))
            } else {
                tvAnoEstado.setTextColor(itemView.context.getResources().getColor(android.R.color.holo_red_light, itemView.context.theme))
            }
        }


        private fun formatoNumeroConSeparadores(numero: Int): String {
            return NumberFormat.getNumberInstance(Locale.getDefault()).format(numero)
        }
    }

    class AutoDiffCallback : DiffUtil.ItemCallback<Auto>() {
        override fun areItemsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            return oldItem.auto_id == newItem.auto_id
        }

        override fun areContentsTheSame(oldItem: Auto, newItem: Auto): Boolean {

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