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

class AutoAdapter(private val listener: OnAutoClickListener) :
    ListAdapter<Auto, AutoAdapter.AutoViewHolder>(AutoDiffCallback()) {

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
        holder.bind(auto, listener)
    }

    class AutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAutoFoto: ImageView = itemView.findViewById(R.id.ivAutoFoto)
        private val tvMarcaModelo: TextView = itemView.findViewById(R.id.tvMarcaModelo)
        private val tvAño: TextView = itemView.findViewById(R.id.tvAño)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val ibEditar: ImageButton = itemView.findViewById(R.id.ibEditar)

        fun bind(auto: Auto, listener: OnAutoClickListener) {
            tvMarcaModelo.text = "${auto.marca} ${auto.modelo}"
            tvAño.text = auto.año.toString()

            // Formatear precio como moneda
            val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            tvPrecio.text = formatoPrecio.format(auto.precio)

            tvEstado.text = auto.estado

            // Cargar imagen si existe
            if (auto.fotoPath.isNotEmpty()) {
                try {
                    ivAutoFoto.setImageURI(Uri.parse(auto.fotoPath))
                } catch (e: Exception) {
                    ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                ivAutoFoto.setImageResource(R.drawable.ic_launcher_foreground)
            }

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
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Auto, newItem: Auto): Boolean {
            return oldItem == newItem
        }
    }
}