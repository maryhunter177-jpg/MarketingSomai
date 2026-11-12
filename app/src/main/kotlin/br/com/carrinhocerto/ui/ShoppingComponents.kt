package br.com.carrinhocerto.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.carrinhocerto.domain.money
import br.com.carrinhocerto.domain.remainingBudget
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppHeader(privacy: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).background(Forest, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { AppIcon(Symbol.Cart, tint = Color.White) }
                Column {
                    Text("Carrinho Certo", style = MaterialTheme.typography.titleLarge)
                    Text("SUA COMPRA, SOB CONTROLE", fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        actions = { IconButton(onClick = privacy) { AppIcon(Symbol.Shield, "Privacidade", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable fun MarketSelector(name: String, select: () -> Unit) {
    Surface(onClick = select, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(Symbol.Store, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("MERCADO ATUAL", fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(name.ifBlank { "Escolher mercado" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AppIcon(Symbol.Chevron, "Trocar mercado", Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun BasketSummary(state: ShopState, budget: Long, editBudget: () -> Unit, manual: () -> Unit, finish: () -> Unit) {
    val exceeded = budget > 0 && state.total > budget
    val warning = budget > 0 && BigDecimal(state.total) >= BigDecimal(budget).multiply(BigDecimal("0.8"))
    val totalColor by animateColorAsState(if (exceeded) Color(0xFFFFC8C2) else if (warning) Color(0xFFFFDF8A) else Color.White, label = "Cor do total")
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF134B3B), Forest)), RoundedCornerShape(24.dp))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL DA COMPRA", color = Mint, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                        Text("${state.cart.size} ${if (state.cart.size == 1) "produto" else "produtos"}", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 11.sp)
                    }
                }
                AnimatedContent(state.total, label = "Total da compra") { cents -> Text(money(cents), style = MaterialTheme.typography.headlineLarge, color = totalColor, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                val units = state.cart.filter { it.item.unit == "un" }.sumOf { it.item.quantity.toLong() }
                val weight = state.cart.filter { it.item.unit == "kg" }.fold(BigDecimal.ZERO) { a, r -> a + r.item.quantity }
                Text("$units ${if (units == 1L) "unidade" else "unidades"}${if (weight.signum() > 0) " • ${weight.stripTrailingZeros().toPlainString().replace('.', ',')} kg" else ""} no carrinho", color = Color(0xFFCEE5D8), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.16f))
                Surface(onClick = editBudget, color = Color.Transparent, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppIcon(Symbol.Wallet, modifier = Modifier.size(18.dp), tint = Mint)
                        Column(Modifier.weight(1f)) {
                            Text(if (budget > 0) "Saldo disponível" else "Quanto você quer gastar?", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            if (budget > 0) Text("Limite: ${money(budget)}", color = Color(0xFFCEE5D8), fontSize = 10.sp)
                            else Text("Defina um valor e acompanhe seu saldo", color = Color(0xFFCEE5D8), fontSize = 10.sp)
                        }
                        if (budget > 0) Text(money(remainingBudget(budget, state.total)), color = if (exceeded) totalColor else Mint, style = MaterialTheme.typography.titleMedium)
                        AppIcon(Symbol.Edit, "Editar orçamento", Modifier.size(16.dp), Mint)
                    }
                }
                if (exceeded) Text("Orçamento ultrapassado em ${money(state.total - budget)}", color = totalColor, style = MaterialTheme.typography.bodySmall)
                else if (warning) Text("Atenção: você chegou a 80% do orçamento.", color = totalColor, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = manual, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                AppIcon(Symbol.Keyboard, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Digitar código", maxLines = 1)
            }
            Button(onClick = finish, enabled = state.cart.isNotEmpty(), modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                AppIcon(Symbol.Check, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Finalizar", maxLines = 1)
            }
        }
    }
}

@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
