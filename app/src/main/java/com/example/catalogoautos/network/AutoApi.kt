package com.example.catalogoautos.network

import retrofit2.Response
import retrofit2.http.*

// Definir un tipo específico para el mapa
typealias AutoMap = HashMap<String, Any>

interface AutoApi {
    /**
     * Obtener todos los autos
     */
    @GET("auto")
    suspend fun obtenerTodos(): Response<List<AutoMap>>

    /**
     * Obtener un auto por su ID
     */
    @GET("auto/{id}")
    suspend fun obtenerPorId(@Path("id") id: Int): Response<AutoMap>

    /**
     * Crear un nuevo auto o actualizar uno existente
     */
    @POST("auto")
    suspend fun agregarAuto(@Body auto: AutoMap): Response<AutoMap>

    /**
     * Actualizar disponibilidad de un auto
     */
    @PUT("auto/{id}/disponibilidad/{disponible}")
    suspend fun actualizarDisponibilidad(
        @Path("id") id: Int,
        @Path("disponible") disponible: Boolean
    ): Response<Map<String, Any>>

    /**
     * Actualizar stock de un auto
     */
    @PUT("auto/{id}/stock/{nuevoStock}")
    suspend fun actualizarStock(
        @Path("id") id: Int,
        @Path("nuevoStock") stock: Int
    ): Response<Map<String, Any>>

    /**
     * Eliminar un auto
     */
    @DELETE("auto/{id}")
    suspend fun eliminarAuto(@Path("id") id: Int): Response<Void>

    /**
     * Verificar si existe un número de serie
     */
    @GET("auto/verificar-serie/{serie}")
    suspend fun verificarNumeroSerie(@Path("serie") serie: String): Response<Map<String, Boolean>>

    /**
     * Verificar si existe un SKU
     */
    @GET("auto/verificar-sku/{sku}")
    suspend fun verificarSku(@Path("sku") sku: String): Response<Map<String, Boolean>>

    /**
     * Buscar autos por modelo
     */
    @GET("auto/buscar/modelo/{modelo}")
    suspend fun buscarPorModelo(@Path("modelo") modelo: String): Response<List<AutoMap>>

    /**
     * Obtener estadísticas de autos (total y disponibles)
     */
    @GET("auto/contar")
    suspend fun contarAutos(): Response<Map<String, Long>>
}