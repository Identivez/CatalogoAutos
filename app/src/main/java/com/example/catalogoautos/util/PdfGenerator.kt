package com.example.catalogoautos.util

import Venta
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import com.example.catalogoautos.R

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {
    private val TAG = "PdfGenerator"
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun generateVentasReport(
        ventas: List<Venta>,
        fechaInicio: LocalDateTime,
        fechaFin: LocalDateTime,
        estatus: String?
    ): File? {
        val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())


        val document = PdfDocument()


        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas


        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }


        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.byd_banner)
        logoBitmap?.let {
            val resizedLogo = Bitmap.createScaledBitmap(it, 100, 50, true)
            canvas.drawBitmap(resizedLogo, 40f, 40f, paint)
        }


        paint.apply {
            textSize = 18f
            isFakeBoldText = true
        }
        canvas.drawText("Reporte de Ventas", 40f, 120f, paint)


        paint.apply {
            textSize = 12f
            isFakeBoldText = false
        }
        val periodoText = "Periodo: ${fechaInicio.format(dateFormatter)} - ${fechaFin.format(dateFormatter)}"
        canvas.drawText(periodoText, 40f, 150f, paint)

        if (!estatus.isNullOrEmpty() && estatus.lowercase() != "todos") {
            canvas.drawText("Estatus: $estatus", 40f, 170f, paint)
        }


        val fechaGeneracion = "Fecha de generación: ${LocalDateTime.now().format(dateFormatter)}"
        canvas.drawText(fechaGeneracion, 40f, 190f, paint)


        var startY = 230f
        val rowHeight = 25f


        paint.isFakeBoldText = true
        canvas.drawText("ID", 40f, startY, paint)
        canvas.drawText("No. Serie", 70f, startY, paint)
        canvas.drawText("Cantidad", 180f, startY, paint)
        canvas.drawText("Precio", 250f, startY, paint)
        canvas.drawText("Estatus", 350f, startY, paint)
        canvas.drawText("Fecha", 450f, startY, paint)


        paint.strokeWidth = 1f
        canvas.drawLine(40f, startY + 5f, 555f, startY + 5f, paint)


        paint.isFakeBoldText = false
        startY += 25f

        var total = BigDecimal.ZERO
        for (venta in ventas) {
            canvas.drawText(venta.ventaId.toString(), 40f, startY, paint)
            canvas.drawText(venta.nSerie, 70f, startY, paint)
            canvas.drawText(venta.cantidad.toString(), 180f, startY, paint)

            val precio = currencyFormat.format(venta.precio)
            canvas.drawText(precio, 250f, startY, paint)

            canvas.drawText(venta.estatus, 350f, startY, paint)
            canvas.drawText(venta.fechaVenta.format(dateFormatter), 450f, startY, paint)

            startY += rowHeight
            total = total.add(venta.precio.multiply(BigDecimal(venta.cantidad)))


            if (startY > 800f) {
                document.finishPage(page)
                val newPage = document.startPage(pageInfo)
                val newCanvas = newPage.canvas
                startY = 40f


                paint.apply {
                    color = Color.BLACK
                    textSize = 12f
                    isFakeBoldText = false
                }
            }
        }


        canvas.drawLine(40f, startY, 555f, startY, paint)
        startY += 20f


        paint.isFakeBoldText = true
        canvas.drawText("Total: ${currencyFormat.format(total)}", 350f, startY, paint)


        document.finishPage(page)


        val fileName = "Ventas_${fileNameDateFormat.format(Date())}.pdf"
        val pdfFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            pdfFile
        } catch (e: IOException) {
            Log.e(TAG, "Error al generar PDF", e)
            document.close()
            null
        }
    }


    fun generateVentasReport(
        ventas: List<Venta>,
        fechaInicio: Date,
        fechaFin: Date,
        estatus: String?
    ): File? {

        val fechaInicioLDT = fechaInicio.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        val fechaFinLDT = fechaFin.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        return generateVentasReport(ventas, fechaInicioLDT, fechaFinLDT, estatus)
    }
}