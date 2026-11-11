package br.com.carrinhocerto.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carrinhocerto.data.*
import br.com.carrinhocerto.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

data class ShopState(val cart: List<CartRow> = emptyList(), val products: List<Product> = emptyList(), val prices: List<Price> = emptyList(), val purchases: List<Purchase> = emptyList(), val stores: List<Store> = emptyList()) {
    val total get() = total(cart.map { it.item.unitPriceCents to it.item.quantity })
}
class ShopViewModel(val repo: ShopRepository) : ViewModel() {
    val state = combine(repo.dao.cart(), repo.dao.products(), repo.dao.prices(), repo.dao.purchases(), repo.dao.stores()) { cart, products, prices, purchases, stores -> ShopState(cart, products, prices, purchases, stores) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopState())
    val pending = MutableStateFlow<Resolved?>(null)
    val busy = MutableStateFlow(false)
    val messages = Channel<String>(Channel.UNLIMITED)
    val accepted = Channel<Unit>(Channel.UNLIMITED)
    private val debounce = ScanDebouncer()
    fun runTask(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { messages.send(e.message ?: "Não foi possível concluir. Tente novamente.") } } }
    fun scan(code: String) {
        if (busy.value || pending.value != null || repo.settings.store.isBlank() || !debounce.accept(code, SystemClock.elapsedRealtime())) return
        busy.value = true
        runTask { try { accepted.send(Unit); pending.value = repo.resolve(code.trim()) } finally { busy.value = false } }
    }
    fun confirm(name: String, cents: Long, unit: String, qty: BigDecimal, replace: Boolean) {
        val resolved = pending.value ?: return
        if (busy.value) return
        busy.value = true
        runTask { try { repo.confirm(resolved.product.copy(name = name.ifBlank { "Produto não identificado" }), cents, unit, qty, replace); pending.value = null; messages.send("${name.ifBlank { "Produto não identificado" }} adicionado - ${money(subtotal(cents, qty))}") } finally { busy.value = false } }
    }
    fun edit(row: CartRow) { pending.value = Resolved(Product(row.item.ean, row.name, imageUrl = row.imageUrl), Price(row.item.ean, repo.settings.store, row.item.unitPriceCents, row.item.unit), row.item) }
    fun change(row: CartRow, delta: BigDecimal) = runTask { repo.change(row.item.id, changedQuantity(row.item.quantity, delta)) }
    fun remove(row: CartRow) = runTask { repo.dao.remove(row.item.id) }
    fun undo(row: CartRow) = runTask { val current = repo.dao.cartItem(row.item.ean); repo.dao.save(if (current == null) row.item else current.copy(quantity = current.quantity + row.item.quantity)) }
}
