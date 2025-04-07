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
import java.util.Locale

class AutoAdapter(
    private val listener: OnAutoClickListener,
    private val marcasMap: Map<Int, String> = emptyMap() // Mapa de IDs de marcas a nombres
) : ListAdapter<Auto, AutoAdapter.AutoViewHolder>(AutoDiffCallback()) {

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
        holder.bind(auto, listener, marcasMap)
    }

    class AutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvAño: TextView = itemView.findViewById(R.id.tvAño)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvDisponibilidad: TextView = itemView.findViewById(R.id.tvDisponibilidad)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val ibEditar: ImageButton = itemView.findViewById(R.id.ibEditar)

        fun bind(auto: Auto, listener: OnAutoClickListener, marcasMap: Map<Int, String>) {
            // Obtener el nombre de la marca desde el mapa usando el marca_id
            val nombreMarca = marcasMap[auto.marca_id] ?: "Marca ${auto.marca_id}"
            tvMarcaModelo.text = "$nombreMarca ${auto.modelo}"
            tvAño.text = auto.año.toString()

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            // Mostrar disponibilidad y stock
            tvDisponibilidad.text = if (auto.disponibilidad) "Disponible" else "No disponible"
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
            return oldItem == newItem
        }
    }

    // Método para actualizar el mapa de marcas
    fun actualizarMarcas(marcas: Map<Int, String>) {
        // Este método podría usarse para actualizar el mapa de marcas
        // si las marcas se cargan después de inicializar el adaptador
    }
}