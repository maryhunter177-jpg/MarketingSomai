package br.com.carrinhocerto.data

import br.com.carrinhocerto.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@Serializable data class FoodResponse(val product: FoodProduct? = null)
@Serializable data class FoodProduct(@SerialName("product_name") val name: String = "", val brands: String? = null, @SerialName("image_front_small_url") val image: String? = null)
interface FoodApi { @GET("api/v2/product/{ean}.json") suspend fun product(@Path("ean") ean: String, @Query("fields") fields: String = "product_name,brands,image_front_small_url"): FoodResponse }
fun retrofit(url: String): Retrofit = Retrofit.Builder().baseUrl(url).client(OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).callTimeout(5, TimeUnit.SECONDS).addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", "CarrinhoCerto/1.0 (Android; consulta de produto)").build()) }.build()).addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())).build()
interface PriceRepository { suspend fun find(ean: String, storeId: String): Price?; suspend fun save(price: Price) }
class LocalPriceRepository(private val dao: ShopDao) : PriceRepository {
    override suspend fun find(ean: String, storeId: String) = dao.price(ean, storeId)
    override suspend fun save(price: Price) = dao.save(price)
}
@Serializable data class SharedPrice(val ean: String, @SerialName("store_id") val storeId: String, val cents: Long, val unit: String, @SerialName("updated_at") val updatedAt: Long, @SerialName("device_id") val deviceId: String)
interface SharedApi {
    @GET("rest/v1/prices") suspend fun find(@Header("apikey") key: String, @Query("ean") ean: String, @Query("store_id") store: String, @Query("order") order: String = "updated_at.desc", @Query("limit") limit: Int = 1, @Header("Authorization") authorization: String = "Bearer ${BuildConfig.SUPABASE_KEY}"): List<SharedPrice>
    @POST("rest/v1/prices") suspend fun send(@Header("apikey") key: String, @Body price: SharedPrice, @Header("Authorization") authorization: String = "Bearer ${BuildConfig.SUPABASE_KEY}")
}
class RemotePriceRepository(private val deviceId: String) : PriceRepository {
    private val api by lazy { retrofit(BuildConfig.SUPABASE_URL.trimEnd('/') + "/").create(SharedApi::class.java) }
    override suspend fun find(ean: String, storeId: String): Price? = api.find(BuildConfig.SUPABASE_KEY, "eq.$ean", "eq.$storeId").firstOrNull()?.let { Price(it.ean, it.storeId, it.cents, it.unit, "compartilhado", it.updatedAt) }
    override suspend fun save(price: Price) { api.send(BuildConfig.SUPABASE_KEY, SharedPrice(price.ean, price.storeId, price.cents, price.unit, price.updatedAt, deviceId)) }
}
