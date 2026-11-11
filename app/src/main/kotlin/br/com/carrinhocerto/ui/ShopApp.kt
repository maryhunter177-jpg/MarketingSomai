package br.com.carrinhocerto.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.carrinhocerto.data.*
import br.com.carrinhocerto.domain.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ShopApp(vm: ShopViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var manual by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    var store by remember { mutableStateOf(vm.repo.settings.store) }
    var chooseStore by remember { mutableStateOf(store.isBlank()) }
    var budgetText by rememberSaveable { mutableStateOf(vm.repo.settings.budget.takeIf { it > 0 }?.let { BigDecimal.valueOf(it, 2).toPlainString().replace('.', ',') } ?: "") }
    var budget by remember { mutableStateOf(vm.repo.settings.budget) }
    var details by remember { mutableStateOf<List<PurchaseItem>?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(vm) { for (message in vm.messages) snack.showSnackbar(message) }
    LaunchedEffect(vm) {
        for (ignored in vm.accepted) {
            runCatching { context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) }
            runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 65).also { it.startTone(ToneGenerator.TONE_PROP_BEEP, 100); delay(150); it.release() } }
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snack) }, topBar = {
        TopAppBar(title = { Text("Carrinho Certo") }, actions = { TextButton(onClick = { privacy = true }) { Text("Privacidade") } })
    }, bottomBar = {
        NavigationBar { listOf("Carrinho", "Histórico", "Produtos").forEachIndexed { index, label -> NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(listOf("▣", "◷", "▤")[index]) }, label = { Text(label) }) } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextButton(onClick = { chooseStore = true }) { Text("Mercado: ${store.ifBlank { "Escolher mercado" }}") }
            when (tab) {
                0 -> {
                    Scanner(ocr = false, active = pending == null && !busy && !chooseStore && !manual && !finish, onCode = vm::scan, onPrices = {}, modifier = Modifier.fillMaxWidth().height(180.dp))
                    val color = when { budget > 0 && state.total > budget -> MaterialTheme.colorScheme.error; budget > 0 && BigDecimal(state.total) >= BigDecimal(budget).multiply(BigDecimal("0.8")) -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFFD54F) else Color(0xFF805500); else -> MaterialTheme.colorScheme.primary }
                    Surface(tonalElevation = 3.dp) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(money(state.total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.animateContentSize())
                            Text("${state.cart.size} produtos • ${state.cart.filter { it.item.unit == "un" }.sumOf { it.item.quantity.toLong() }} unidades • ${state.cart.filter { it.item.unit == "kg" }.fold(BigDecimal.ZERO) { a, r -> a + r.item.quantity }.stripTrailingZeros().toPlainString()} kg")
                            if (budget > 0 && state.total > budget) Text("Você ultrapassou seu orçamento.", color = color)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(budgetText, { budgetText = it; budget = parsePrice(it) ?: 0; vm.repo.settings.budget = budget }, label = { Text("Meu orçamento") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                                TextButton(onClick = { manual = true }) { Text("Digitar código") }
                            }
                            Button(onClick = { finish = true }, enabled = state.cart.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Finalizar compra") }
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    LazyColumn(Modifier.weight(1f)) {
                        if (state.cart.isEmpty()) item { Text("Seu carrinho está vazio. Escaneie um produto para começar.", Modifier.padding(24.dp)) }
                        items(state.cart, key = { it.item.id }) { row ->
                            val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    vm.remove(row)
                                    scope.launch { if (snack.showSnackbar("${row.name} removido", "Desfazer", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) vm.undo(row) }
                                }
                                false
                            })
                            SwipeToDismissBox(state = dismiss, backgroundContent = { Box(Modifier.fillMaxSize().padding(16.dp)) { Text("Remover", color = MaterialTheme.colorScheme.error) } }) {
                                CartCard(row, { vm.edit(row) }, { vm.change(row, it) }, {
                                    vm.remove(row)
                                    scope.launch { if (snack.showSnackbar("Produto removido", "Desfazer") == SnackbarResult.ActionPerformed) vm.undo(row) }
                                })
                            }
                        }
                    }
                }
                1 -> LazyColumn {
                    if (state.purchases.isEmpty()) item { Text("Suas compras finalizadas aparecerão aqui.", Modifier.padding(24.dp)) }
                    items(state.purchases, key = { it.id }) { purchase ->
                        ListItem(headlineContent = { Text(money(purchase.totalCents)) }, supportingContent = { Text("${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(purchase.date))} • ${state.stores.find { it.id == purchase.storeId }?.name ?: purchase.storeId}") }, modifier = Modifier.clickable { vm.runTask { details = vm.repo.dao.details(purchase.id) } })
                        HorizontalDivider()
                    }
                }
                2 -> ProductsScreen(state, vm)
            }
        }
    }
    if (manual) TextDialog("Código do produto", "Código de barras", "", KeyboardType.Ascii, { manual = false }) { code -> manual = false; vm.scan(code) }
    if (chooseStore) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (store.isNotBlank()) chooseStore = false }, title = { Text("Escolha seu mercado") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Use o mesmo nome para consultar preços desse mercado. Esvazie o carrinho antes de trocar.")
                state.stores.forEach { saved -> TextButton(onClick = { name = saved.name }) { Text(saved.name) } }
                OutlinedTextField(name, { name = it }, label = { Text("Nome e bairro do mercado") })
            }
        }, confirmButton = { TextButton(enabled = name.isNotBlank() && (state.cart.isEmpty() || name.trim().lowercase() == store), onClick = {
            val id = name.trim().lowercase(Locale.ROOT)
            vm.runTask { vm.repo.dao.save(Store(id, name.trim())); vm.repo.settings.store = id; store = id; chooseStore = false }
        }) { Text("Usar mercado") } }, dismissButton = { if (store.isNotBlank()) TextButton(onClick = { chooseStore = false }) { Text("Cancelar") } })
    }
    if (finish) AlertDialog(onDismissRequest = { finish = false }, title = { Text("Finalizar compra?") }, text = { Text("Salvar ${money(state.total)} no histórico e esvaziar o carrinho?") }, confirmButton = { TextButton(onClick = { finish = false; vm.runTask { vm.repo.finish(); vm.messages.send("Compra salva no histórico.") } }) { Text("Finalizar") } }, dismissButton = { TextButton(onClick = { finish = false }) { Text("Cancelar") } })
    details?.let { lines -> AlertDialog(onDismissRequest = { details = null }, title = { Text("Detalhes da compra") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { lines.forEach { Text("${it.name}\n${it.quantity.toPlainString()} ${it.unit} × ${money(it.unitPriceCents)} = ${money(subtotal(it.unitPriceCents, it.quantity))}", Modifier.padding(bottom = 12.dp)) } } }, confirmButton = { TextButton(onClick = { details = null }) { Text("Fechar") } }) }
    if (privacy) {
        var sharing by remember { mutableStateOf(vm.repo.settings.sharing) }
        AlertDialog(onDismissRequest = { privacy = false }, title = { Text("Privacidade") }, text = {
            Column {
                Text("Sem login e sem anúncios. Carrinho, produtos e histórico ficam neste aparelho. O código consultado é enviado ao Open Food Facts; fotos de produtos são carregadas do endereço retornado. A câmera e o OCR processam imagens no aparelho, sem enviá-las. O ML Kit pode enviar métricas técnicas ao Google conforme seus termos.")
                Text("O compartilhamento opcional envia código, preço, unidade, mercado, data e um identificador aleatório persistente ao servidor configurado. Nenhuma foto de etiqueta é enviada. Desligar impede novos envios, mas não apaga registros já enviados.")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(sharing, enabled = vm.repo.settings.configured, onCheckedChange = { sharing = it; vm.repo.settings.sharing = it }); Text("Compartilhar preços") }
                if (!vm.repo.settings.configured) Text("Servidor opcional não configurado. Compartilhamento desligado.")
            }
        }, confirmButton = { TextButton(onClick = { privacy = false }) { Text("Fechar") } })
    }
    pending?.let { resolved -> key(resolved.product.ean) { Confirmation(resolved, busy, { if (!busy) vm.pending.value = null }, vm::confirm) } }
}

@Composable private fun CartCard(row: CartRow, edit: () -> Unit, change: (BigDecimal) -> Unit, remove: () -> Unit) {
    Surface { Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            row.imageUrl?.let { AsyncImage(it, "Foto de ${row.name}", Modifier.size(56.dp)) }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(row.name, fontWeight = FontWeight.Bold)
                Text("${money(row.item.unitPriceCents)}/${row.item.unit} • Corrigir preço", Modifier.clickable(onClick = edit), color = MaterialTheme.colorScheme.primary)
                if (row.previousPrice != null && row.previousPrice != row.item.unitPriceCents) Text("Última vez: ${money(row.previousPrice)}", style = MaterialTheme.typography.bodySmall)
            }
            Text(money(subtotal(row.item.unitPriceCents, row.item.quantity)), fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val step = if (row.item.unit == "kg") BigDecimal("0.100") else BigDecimal.ONE
            OutlinedButton(onClick = { change(step.negate()) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("−") }
            TextButton(onClick = edit) { Text("${row.item.quantity.stripTrailingZeros().toPlainString().replace('.', ',')} ${row.item.unit}") }
            OutlinedButton(onClick = { change(step) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("+") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = remove) { Text("Remover") }
        }
        HorizontalDivider()
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Confirmation(resolved: Resolved, busy: Boolean, close: () -> Unit, confirm: (String, Long, String, BigDecimal, Boolean) -> Unit) {
    val fresh = resolved.price?.let { System.currentTimeMillis() - it.updatedAt in 0 until 30L * 86400000 } == true
    var name by rememberSaveable { mutableStateOf(resolved.product.name) }
    var price by rememberSaveable { mutableStateOf(resolved.price?.let { BigDecimal.valueOf(it.cents, 2).toPlainString().replace('.', ',') } ?: "") }
    var unit by rememberSaveable { mutableStateOf(resolved.current?.unit ?: resolved.price?.unit ?: "un") }
    var qty by rememberSaveable { mutableStateOf(resolved.current?.quantity?.toPlainString() ?: "1") }
    var replace by rememberSaveable { mutableStateOf(resolved.current != null) }
    var ocr by rememberSaveable { mutableStateOf(!fresh) }
    var candidates by remember { mutableStateOf<List<PriceCandidate>>(emptyList()) }
    var ocrMessage by remember { mutableStateOf("") }
    val cents = parsePrice(price)
    val amount = quantity(qty, unit)
    ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Confirmar produto", style = MaterialTheme.typography.titleLarge)
            resolved.product.imageUrl?.let { AsyncImage(it, "Foto do produto", Modifier.size(64.dp)) }
            OutlinedTextField(name, { name = it }, label = { Text("Nome do produto") }, modifier = Modifier.fillMaxWidth())
            resolved.price?.let { val days = ((System.currentTimeMillis() - it.updatedAt) / 86400000).coerceAtLeast(0); Text("${if (it.source == "local") "Você registrou" else "Outro usuário registrou"} • Atualizado há $days dias${if (!fresh) " • Confira a etiqueta" else ""}") }
            if (resolved.price != null) TextButton(onClick = { price = ""; ocr = true }) { Text("Esse preço está errado") }
            if (ocr) {
                Scanner(true, !busy, {}, { candidates = it; ocrMessage = if (it.isEmpty()) "Nenhum preço encontrado. Tente novamente ou digite." else "Toque no preço da etiqueta para usar." }, Modifier.fillMaxWidth().height(230.dp))
                Text(ocrMessage)
                candidates.forEach { candidate -> OutlinedButton(onClick = { price = BigDecimal.valueOf(candidate.cents, 2).toPlainString().replace('.', ','); unit = candidate.unit }) { Text("Usar ${money(candidate.cents)}/${candidate.unit}") } }
                TextButton(onClick = { ocr = false }) { Text("Digitar preço manualmente") }
            } else TextButton(onClick = { ocr = true }) { Text("Foto da etiqueta") }
            OutlinedTextField(price, { price = it }, label = { Text(if (unit == "kg") "Preço por kg (R$)" else "Preço unitário (R$)") }, supportingText = { Text("12,90 ou 12.90 = R$ 12,90; 1290 = R$ 12,90") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), isError = price.isNotBlank() && cents == null)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(unit == "kg", { unit = if (it) "kg" else "un"; qty = "1" }); Text("Vendido por kg") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val step = if (unit == "kg") BigDecimal("0.100") else BigDecimal.ONE
                OutlinedButton(onClick = { qty = ((amount ?: BigDecimal.ONE) - step).max(step).toPlainString() }) { Text("−") }
                OutlinedTextField(qty, { qty = it }, label = { Text("Quantidade ($unit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), isError = amount == null)
                OutlinedButton(onClick = { qty = ((amount ?: BigDecimal.ZERO) + step).min(BigDecimal("9999")).toPlainString() }) { Text("+") }
            }
            if (resolved.current != null) {
                Text("No carrinho: ${resolved.current.quantity.toPlainString()} ${resolved.current.unit}")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(replace, { replace = it; qty = if (it) resolved.current.quantity.toPlainString() else "1" }); Text("Substituir quantidade atual (desmarque para somar)") }
            }
            if (cents != null && amount != null) Text("Subtotal: ${money(subtotal(cents, amount))}", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { if (cents != null && amount != null) confirm(name, cents, unit, amount, replace) }, enabled = !busy && cents != null && amount != null, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (busy) "Salvando…" else "Adicionar ao carrinho") }
            TextButton(onClick = close, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
    }
}

@Composable private fun ProductsScreen(state: ShopState, vm: ShopViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    Column {
        OutlinedTextField(search, { search = it }, label = { Text("Pesquisar nome ou código") }, modifier = Modifier.fillMaxWidth().padding(12.dp))
        LazyColumn {
            items(state.products.filter { it.name.contains(search, true) || it.ean.contains(search) }, key = { it.ean }) { product ->
                val price = state.prices.find { it.ean == product.ean && it.storeId == vm.repo.settings.store }
                ListItem(headlineContent = { Text(product.name) }, supportingContent = { Text("${product.ean} • ${price?.let { money(it.cents) + "/" + it.unit } ?: "Sem preço neste mercado"}") }, trailingContent = { Row { TextButton(onClick = { editing = product }) { Text("Editar") }; TextButton(onClick = { deleting = product }) { Text("Excluir") } } })
                HorizontalDivider()
            }
        }
    }
    editing?.let { product ->
        var name by remember { mutableStateOf(product.name) }
        val old = state.prices.find { it.ean == product.ean && it.storeId == vm.repo.settings.store }
        var price by remember { mutableStateOf(old?.let { BigDecimal.valueOf(it.cents, 2).toPlainString() } ?: "") }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Editar produto e preço") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Nome") }); OutlinedTextField(price, { price = it }, label = { Text("Preço (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); Text("A correção vale para próximas leituras. Para corrigir o carrinho, toque no preço do item.") } }, confirmButton = { TextButton(enabled = name.isNotBlank() && (price.isBlank() || parsePrice(price) != null), onClick = { vm.runTask { vm.repo.editProduct(product.copy(name = name), parsePrice(price), old?.unit ?: "un"); editing = null } }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancelar") } })
    }
    deleting?.let { product -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Excluir produto salvo?") }, text = { Text("Excluir ${product.name} e seus preços? O histórico será preservado.") }, confirmButton = { TextButton(onClick = { vm.runTask { vm.repo.delete(product.ean); deleting = null } }) { Text("Excluir") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}

@Composable private fun TextDialog(title: String, label: String, initial: String, keyboard: KeyboardType, close: () -> Unit, save: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard), singleLine = true) }, confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = { save(value) }) { Text("Continuar") } }, dismissButton = { TextButton(onClick = close) { Text("Cancelar") } })
}
