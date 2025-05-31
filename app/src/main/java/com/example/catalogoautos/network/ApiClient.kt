package com.example.catalogoautos.network

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

object ApiClient {


    private const val BASE_URL = "http://10.250.3.8:8080/AE_BYD/api/"


    const val AUTO_ENDPOINT = "auto"
    const val USER_ENDPOINT = "usuario"
    const val LOGIN_ENDPOINT = "$USER_ENDPOINT/login"
    const val VENTAS_ENDPOINT = "ventas"


    fun getFullUrl(endpoint: String): String = "$BASE_URL$endpoint"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()


    private class BigDecimalSerializer : JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {
        override fun serialize(
            src: BigDecimal?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return JsonPrimitive(src?.toDouble() ?: 0.0)
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): BigDecimal {
            return try {
                if (json?.isJsonPrimitive == true) {
                    when {
                        json.asJsonPrimitive.isNumber -> BigDecimal(json.asDouble)
                        json.asJsonPrimitive.isString -> BigDecimal(json.asString)
                        else -> BigDecimal.ZERO
                    }
                } else {
                    BigDecimal.ZERO
                }
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
        }
    }


    private val gson = GsonBuilder()
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalSerializer())
        .create()


    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(httpClient)
        .build()


    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val autoApi: AutoApi = retrofit.create(AutoApi::class.java)
    val ventasApi: VentasApi = retrofit.create(VentasApi::class.java)


    fun getBaseUrl(): String {
        return BASE_URL
    }
}