// VentasAdapter.kt
package com.example.catalogoautos.view

import Venta
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class VentasAdapter(
    private var ventas: List<Venta> = emptyList(),
    private val onItemClick: (Venta) -> Unit
) : RecyclerView.Adapter<VentasAdapter.VentaViewHolder>() {

    class VentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNSerie: TextView = itemView.findViewById(R.id.tvNSerie)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val tvEstatus: TextView = itemView.findViewById(R.id.tvEstatus)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        val venta = ventas[position]

        // Formatear datos
        val formatoPrecio = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        holder.tvNSerie.text = "Auto: ${venta.nSerie}"
        holder.tvCantidad.text = "Cantidad: ${venta.cantidad}"
        holder.tvPrecio.text = "Precio: ${formatoPrecio.format(venta.precio)}"
        holder.tvEstatus.text = "Estado: ${venta.estatus}"
        holder.tvFecha.text = "Fecha: ${venta.fechaVenta.format(formatoFecha)}"

        // Configurar colores según el estado
        val context = holder.itemView.context
        when (venta.estatus) {
            "COMPLETADA" -> holder.tvEstatus.setTextColor(context.getColor(android.R.color.holo_green_dark))
            "PENDIENTE" -> holder.tvEstatus.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            "CANCELADA" -> holder.tvEstatus.setTextColor(context.getColor(android.R.color.holo_red_dark))
            else -> holder.tvEstatus.setTextColor(context.getColor(android.R.color.darker_gray))
        }

        // Configurar click listener
        holder.itemView.setOnClickListener {
            onItemClick(venta)
        }
    }

    override fun getItemCount() = ventas.size

    fun actualizarVentas(nuevasVentas: List<Venta>) {
        ventas = nuevasVentas
        notifyDataSetChanged()
    }
}