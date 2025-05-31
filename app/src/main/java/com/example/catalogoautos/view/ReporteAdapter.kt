package com.example.catalogoautos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catalogoautos.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReporteAdapter(
    private val reportes: List<File>,
    private val listener: OnReporteClickListener
) : RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    interface OnReporteClickListener {
        fun onReporteClick(reporte: File)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val reporte = reportes[position]
        holder.bind(reporte, listener)
    }

    override fun getItemCount(): Int = reportes.size

    class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreReporte: TextView = itemView.findViewById(R.id.tvNombreReporte)
        private val tvFechaReporte: TextView = itemView.findViewById(R.id.tvFechaReporte)

        fun bind(reporte: File, listener: OnReporteClickListener) {

            val nombreArchivo = reporte.name
            try {
                if (nombreArchivo.startsWith("Ventas_") && nombreArchivo.length > 21) {
                    val fechaStr = nombreArchivo.substring(7, 21) // Extraer yyyyMMdd_HHmmss

                    val parser = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val fecha = parser.parse(fechaStr)
                    tvFechaReporte.text = "Generado: ${formatter.format(fecha)}"
                } else {
                    tvFechaReporte.text = "Generado: ${Date(reporte.lastModified())}"
                }
            } catch (e: Exception) {
                tvFechaReporte.text = "Generado: ${Date(reporte.lastModified())}"
            }

            tvNombreReporte.text = "Reporte de ventas"

            itemView.setOnClickListener { listener.onReporteClick(reporte) }
        }
    }
}