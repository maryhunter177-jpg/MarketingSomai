package br.com.carrinhocerto.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Entity data class Product(@PrimaryKey val ean: String, val name: String, val brand: String? = null, val imageUrl: String? = null, val updatedAt: Long = System.currentTimeMillis())
@Entity(primaryKeys = ["ean", "storeId"]) data class Price(val ean: String, val storeId: String, val cents: Long, val unit: String, val source: String = "local", val updatedAt: Long = System.currentTimeMillis())
@Entity data class Store(@PrimaryKey val id: String, val name: String)
@Entity(indices = [Index(value = ["ean"], unique = true)]) data class CartItem(@PrimaryKey(autoGenerate = true) val id: Long = 0, val ean: String, val quantity: BigDecimal, val unit: String, val unitPriceCents: Long)
@Entity data class Purchase(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: Long, val storeId: String, val totalCents: Long)
@Entity(primaryKeys = ["purchaseId", "ean"]) data class PurchaseItem(val purchaseId: Long, val ean: String, val name: String, val quantity: BigDecimal, val unit: String, val unitPriceCents: Long)
class Converters {
    @TypeConverter fun decimal(value: String): BigDecimal = BigDecimal(value)
    @TypeConverter fun string(value: BigDecimal): String = value.toPlainString()
}
data class CartRow(@Embedded val item: CartItem, val name: String, val imageUrl: String?, val previousPrice: Long?)
@Dao interface ShopDao {
    @Query("SELECT CartItem.*, COALESCE(Product.name, 'Produto não identificado') AS name, Product.imageUrl, (SELECT unitPriceCents FROM PurchaseItem JOIN Purchase ON Purchase.id = PurchaseItem.purchaseId WHERE PurchaseItem.ean = CartItem.ean ORDER BY Purchase.date DESC LIMIT 1) AS previousPrice FROM CartItem LEFT JOIN Product ON Product.ean = CartItem.ean ORDER BY CartItem.id DESC") fun cart(): Flow<List<CartRow>>
    @Query("SELECT * FROM Product ORDER BY name") fun products(): Flow<List<Product>>
    @Query("SELECT * FROM Price") fun prices(): Flow<List<Price>>
    @Query("SELECT * FROM Store ORDER BY name") fun stores(): Flow<List<Store>>
    @Query("SELECT * FROM Purchase ORDER BY date DESC") fun purchases(): Flow<List<Purchase>>
    @Query("SELECT * FROM PurchaseItem WHERE purchaseId = :id") suspend fun details(id: Long): List<PurchaseItem>
    @Query("SELECT * FROM Product WHERE ean = :ean") suspend fun product(ean: String): Product?
    @Query("SELECT * FROM Price WHERE ean = :ean AND storeId = :store") suspend fun price(ean: String, store: String): Price?
    @Query("SELECT * FROM CartItem WHERE ean = :ean") suspend fun cartItem(ean: String): CartItem?
    @Query("SELECT * FROM CartItem") suspend fun cartSnapshot(): List<CartItem>
    @Upsert suspend fun save(product: Product)
    @Upsert suspend fun save(price: Price)
    @Upsert suspend fun save(store: Store)
    @Upsert suspend fun save(item: CartItem)
    @Insert suspend fun insert(purchase: Purchase): Long
    @Insert suspend fun insert(items: List<PurchaseItem>)
    @Query("DELETE FROM CartItem WHERE id = :id") suspend fun remove(id: Long)
    @Query("DELETE FROM CartItem") suspend fun clearCart()
    @Query("DELETE FROM Price WHERE ean = :ean") suspend fun deletePrices(ean: String)
    @Query("DELETE FROM Product WHERE ean = :ean") suspend fun deleteProduct(ean: String)
}
@Database(entities = [Product::class, Price::class, Store::class, CartItem::class, Purchase::class, PurchaseItem::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ShopDatabase : RoomDatabase() { abstract fun dao(): ShopDao }
