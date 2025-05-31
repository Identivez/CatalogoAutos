import java.math.BigDecimal
import java.time.LocalDateTime


data class Venta(
    val ventaId: Int = 0,
    val nSerie: String,
    val cantidad: Int = 1,
    val precio: BigDecimal,
    val estatus: String = "PENDIENTE",
    val fechaVenta: LocalDateTime = LocalDateTime.now()
) {
    companion object {

        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_COMPLETADA = "COMPLETADA"
        const val ESTADO_ENTREGADA = "ENTREGADA"
        const val ESTADO_CANCELADA = "CANCELADA"


        val ESTADOS_VALIDOS = listOf(ESTADO_PENDIENTE, ESTADO_COMPLETADA, ESTADO_ENTREGADA, ESTADO_CANCELADA)

       
    }
}