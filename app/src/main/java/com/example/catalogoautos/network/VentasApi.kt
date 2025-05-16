package com.example.catalogoautos.network

import com.example.catalogoautos.model.VentaRequest
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDateTime

interface VentasApi {
    /**
     * Obtener todas las ventas
     */
    @GET(ApiClient.VENTAS_ENDPOINT)
    suspend fun obtenerTodasLasVentas(): Response<List<Map<String, Any>>>

    /**
     * Obtener venta por ID
     */
    @GET("${ApiClient.VENTAS_ENDPOINT}/{id}")
    suspend fun obtenerVentaPorId(@Path("id") id: Int): Response<Map<String, Any>>

    /**
     * Obtener ventas por estatus
     */
    @GET("${ApiClient.VENTAS_ENDPOINT}/estatus/{estatus}")
    suspend fun obtenerVentasPorEstatus(@Path("estatus") estatus: String): Response<List<Map<String, Any>>>

    /**
     * Registrar una nueva venta
     * Ahora usando una clase específica en lugar de Map
     */
    @POST(ApiClient.VENTAS_ENDPOINT)
    suspend fun registrarVenta(@Body venta: VentaRequest): Response<Map<String, Any>>

    /**
     * Actualizar estatus de una venta
     */
    @PUT("${ApiClient.VENTAS_ENDPOINT}/{id}/estatus")
    suspend fun actualizarEstatus(@Path("id") id: Int, @Body estatus: Map<String, String>): Response<Map<String, Any>>

    /**
     * Cancelar una venta
     */
    @DELETE("${ApiClient.VENTAS_ENDPOINT}/{id}")
    suspend fun cancelarVenta(@Path("id") id: Int): Response<Map<String, String>>

    /**
     * Contar ventas por estatus
     */
    @GET("${ApiClient.VENTAS_ENDPOINT}/contar")
    suspend fun contarVentas(): Response<Map<String, Any>>

    /**
     * Obtener ventas por filtro de fecha y estatus
     * @param fechaInicio Fecha de inicio en formato ISO (yyyy-MM-dd)
     * @param fechaFin Fecha fin en formato ISO (yyyy-MM-dd)
     * @param estatus Estatus opcional para filtrar (puede ser null)
     */
    @GET("${ApiClient.VENTAS_ENDPOINT}/filtro")
    suspend fun obtenerVentasPorFiltro(
        @Query("fechaInicio") fechaInicio: String,
        @Query("fechaFin") fechaFin: String,
        @Query("estatus") estatus: String?
    ): Response<List<Map<String, Any>>>
}