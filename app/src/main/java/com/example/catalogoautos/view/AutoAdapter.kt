package com.example.catalogoautos.view

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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

class AutoAdapter(
    private val listener: OnAutoClickListener
) : ListAdapter<Auto, AutoAdapter.AutoViewHolder>(AutoDiffCallback()) {

    // Constante para el nombre de la marca BYD
    private val MARCA_BYD = "BYD"

    interface OnAutoClickListener {
        fun onAutoClick(auto: Auto)
        fun onEditClick(auto: Auto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auto, parent, false)
        return AutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AutoViewHolder, position: Int) {
        val auto = getItem(position)
        holder.bind(auto, listener, MARCA_BYD)
    }

    class AutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvSku: TextView = itemView.findViewById(R.id.tvSku)
        private val tvAnio: TextView = itemView.findViewById(R.id.tvAnio)
        private val tvColor: TextView = itemView.findViewById(R.id.tvColor)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvDisponibilidad: TextView = itemView.findViewById(R.id.tvDisponibilidad)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val ibEditar: ImageButton = itemView.findViewById(R.id.ibEditar)

        fun bind(auto: Auto, listener: OnAutoClickListener, marcaNombre: String) {
            // Configurar información básica del auto
            tvMarcaModelo.text = "$marcaNombre ${auto.modelo}"
            tvSku.text = "SKU: ${auto.sku}"
            tvAnio.text = auto.anio.toString()
            tvColor.text = auto.color

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            // Configurar disponibilidad con estilo visual adecuado
            if (auto.disponibilidad) {
                tvDisponibilidad.text = "Disponible"
                tvDisponibilidad.setBackgroundResource(R.drawable.disponible_badge)
                // Cambiamos la forma de acceder al color
                tvDisponibilidad.setTextColor(itemView.context.getResources().getColor(R.color.green, itemView.context.theme))
            } else {
                tvDisponibilidad.text = "No disponible"
                // Cambiamos al drawable genérico, deberás crear este drawable
                tvDisponibilidad.setBackgroundResource(R.drawable.badge_background)
                // Cambiamos la forma de acceder al color
                tvDisponibilidad.setTextColor(itemView.context.getResources().getColor(R.color.red, itemView.context.theme))
            }

            // Mostrar información de stock
            tvStock.text = "Stock: ${auto.stock}"

            // Para las fotos, usaremos una imagen predeterminada por ahora
            // La implementación completa requeriría acceder a la entidad Foto relacionada
            ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)

            // Configurar listeners
            itemView.setOnClickListener {
                listener.onAutoClick(auto)
            }

            ibEditar.setOnClickListener {
                listener.onEditClick(auto)
            }
        }
    }

    class AutoDiffCallback : DiffUtil.ItemCallback<Auto>() {
        override fun areItemsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            return oldItem.auto_id == newItem.auto_id
        }

        override fun areContentsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            // Comparación completa incluyendo los nuevos campos
            return oldItem.auto_id == newItem.auto_id &&
                    oldItem.n_serie == newItem.n_serie &&
                    oldItem.sku == newItem.sku &&
                    oldItem.marca_id == newItem.marca_id &&
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