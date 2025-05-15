import java.math.BigDecimal
import java.time.LocalDateTime

// En app/src/main/java/com/example/catalogoautos/model/Venta.kt
data class Venta(
    val ventaId: Int = 0,
    val nSerie: String,
    val cantidad: Int = 1,
    val precio: BigDecimal,
    // Cambia el valor por defecto a "PENDIENTE" en lugar de "COMPLETADA"
    val estatus: String = "PENDIENTE",
    val fechaVenta: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        // Define constantes para los estados permitidos
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_COMPLETADA = "COMPLETADA"
        const val ESTADO_ENTREGADA = "ENTREGADA"
        const val ESTADO_CANCELADA = "CANCELADA"

        // Lista de estados válidos según la base de datos
        val ESTADOS_VALIDOS = listOf(ESTADO_PENDIENTE, ESTADO_COMPLETADA, ESTADO_ENTREGADA, ESTADO_CANCELADA)

        // Resto del código...
    }
}