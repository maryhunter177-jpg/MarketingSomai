package br.com.carrinhocerto.data

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import br.com.carrinhocerto.BuildConfig
import br.com.carrinhocerto.domain.*
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.TimeUnit

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
    var store: String get() = prefs.getString("mercado", "")!!; set(value) { prefs.edit().putString("mercado", value).apply() }
    var budget: Long get() = prefs.getLong("orcamento", 0); set(value) { prefs.edit().putLong("orcamento", value).apply() }
    var sharing: Boolean get() = configured && prefs.getBoolean("compartilhar", false); set(value) { prefs.edit().putBoolean("compartilhar", value).apply() }
    val configured get() = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_KEY.isNotBlank()
    val deviceId: String get() = prefs.getString("dispositivo", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("dispositivo", it).apply() }
}
data class Resolved(val product: Product, val price: Price?, val current: CartItem?)
class ShopRepository(private val db: ShopDatabase, private val api: FoodApi, val settings: Settings, private val context: Context) {
    val dao = db.dao()
    private val local: PriceRepository = LocalPriceRepository(dao)
    suspend fun resolve(code: String): Resolved {
        require(code.length in 1..80 && code.all { it.code in 32..126 }) { "Código inválido. Digite até 80 caracteres." }
        var product = dao.product(code)
        var price = local.find(code, settings.store)
        if (price == null && settings.sharing) price = optional { RemotePriceRepository(settings.deviceId).find(code, settings.store) }
        if (product == null) {
            val food = optional { api.product(code).product }
            product = Product(code, food?.name?.takeIf { it.isNotBlank() } ?: "Produto não identificado", food?.brands, food?.image)
        }
        return Resolved(product, price, dao.cartItem(code))
    }
    suspend fun confirm(product: Product, cents: Long, unit: String, amount: BigDecimal, replace: Boolean) {
        val price = Price(product.ean, settings.store, cents, unit)
        db.withTransaction {
            dao.save(product.copy(updatedAt = System.currentTimeMillis())); local.save(price)
            val old = dao.cartItem(product.ean)
            val qty = if (!replace && old != null && old.unit == unit) old.quantity + amount else amount
            require(qty <= BigDecimal("9999")) { "Quantidade máxima: 9999." }
            dao.save(CartItem(old?.id ?: 0, product.ean, qty, unit, cents))
        }
        if (settings.sharing) {
            val work = OneTimeWorkRequestBuilder<SharePriceWorker>().setInputData(workDataOf("ean" to price.ean, "store" to price.storeId, "cents" to cents, "unit" to unit, "date" to price.updatedAt)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
    suspend fun change(id: Long, amount: BigDecimal?) = db.withTransaction {
        val item = dao.cartSnapshot().find { it.id == id } ?: return@withTransaction
        if (amount == null || amount <= BigDecimal.ZERO) dao.remove(id) else { require(amount <= BigDecimal("9999")); dao.save(item.copy(quantity = amount)) }
    }
    suspend fun finish() = db.withTransaction {
        val items = dao.cartSnapshot()
        require(items.isNotEmpty()) { "Seu carrinho está vazio." }
        val purchaseId = dao.insert(Purchase(date = System.currentTimeMillis(), storeId = settings.store, totalCents = total(items.map { it.unitPriceCents to it.quantity })))
        dao.insert(items.map { PurchaseItem(purchaseId, it.ean, dao.product(it.ean)?.name ?: "Produto não identificado", it.quantity, it.unit, it.unitPriceCents) })
        dao.clearCart()
    }
    suspend fun delete(ean: String) = db.withTransaction { dao.deletePrices(ean); dao.deleteProduct(ean) }
    suspend fun editProduct(product: Product, cents: Long?, unit: String) = db.withTransaction {
        dao.save(product.copy(updatedAt = System.currentTimeMillis()))
        if (cents != null) dao.save(Price(product.ean, settings.store, cents, unit))
    }
}
private suspend fun <T> optional(block: suspend () -> T): T? = try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
class SharePriceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        if (!settings.sharing) return Result.success()
        return try {
            RemotePriceRepository(settings.deviceId).save(Price(inputData.getString("ean")!!, inputData.getString("store")!!, inputData.getLong("cents", 0), inputData.getString("unit")!!, updatedAt = inputData.getLong("date", 0)))
            Result.success()
        } catch (e: CancellationException) { throw e } catch (_: Exception) { if (runAttemptCount < 5) Result.retry() else Result.failure() }
    }
}
