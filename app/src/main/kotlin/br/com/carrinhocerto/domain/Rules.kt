package br.com.carrinhocerto.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(cents, 2))
fun parsePrice(text: String): Long? = runCatching {
    val clean = text.trim().replace("R$", "").replace(" ", "")
    require(clean.matches(Regex("\\d+([.,]\\d{3})*([.,]\\d{2})?")))
    val decimal = when { clean.contains(',') -> clean.replace(".", "").replace(',', '.'); clean.contains('.') -> clean; else -> return@runCatching clean.toLong().takeIf { it in 1..999999999 } }
    BigDecimal(decimal).movePointRight(2).longValueExact().takeIf { it in 1..999999999 }
}.getOrNull()
fun quantity(text: String, unit: String): BigDecimal? = runCatching {
    BigDecimal(text.replace(',', '.')).also { require(it > BigDecimal.ZERO && it <= BigDecimal("9999") && it.scale() <= if (unit == "kg") 3 else 0) }
}.getOrNull()
fun subtotal(cents: Long, amount: BigDecimal): Long = BigDecimal.valueOf(cents).multiply(amount).setScale(0, RoundingMode.HALF_UP).longValueExact()
fun total(lines: List<Pair<Long, BigDecimal>>): Long = lines.fold(0L) { sum, (price, qty) -> Math.addExact(sum, subtotal(price, qty)) }
fun changedQuantity(current: BigDecimal, delta: BigDecimal): BigDecimal? = current.add(delta).takeIf { it > BigDecimal.ZERO }
class ScanDebouncer(private val interval: Long = 2000) {
    private val seen = mutableMapOf<String, Long>()
    @Synchronized fun accept(code: String, now: Long): Boolean {
        seen.entries.removeAll { now - it.value >= interval }
        if (seen.containsKey(code)) return false
        seen[code] = now
        return true
    }
}

data class TagLine(val text: String, val height: Int = 1, val centrality: Int = 0)
data class PriceCandidate(val cents: Long, val unit: String, val score: Int)
class PriceTagParser {
    private val pattern = Regex("(?<![\\d.,])(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d{1,6}\\.\\d{2}|\\d{1,5}\\s+\\d{2})(?!\\d)", RegexOption.IGNORE_CASE)
    fun parse(text: String): List<PriceCandidate> = parse(text.lines().map { TagLine(it) })
    fun parse(lines: List<TagLine>): List<PriceCandidate> {
        val weight = lines.any { Regex("(?i)(/\\s*kg|\\bkg\\b|100\\s*g\\b)").containsMatchIn(it.text) }
        return lines.flatMap { line ->
            pattern.findAll(line.text).mapNotNull { match ->
                val tail = line.text.substring(match.range.last + 1).trimStart()
                if (Regex("(?i)^(%|g\\b|ml\\b|un\\b|x\\b|/\\d)").containsMatchIn(tail)) return@mapNotNull null
                val raw = match.groupValues[1].replace(Regex("\\s+"), ",")
                val parsed = parsePrice(raw) ?: return@mapNotNull null
                val cents = if (Regex("(?i)100\\s*g\\b").containsMatchIn(lines.joinToString(" ") { it.text })) Math.multiplyExact(parsed, 10) else parsed
                val prefix = line.text.substring(0, match.range.first).lowercase()
                val score = line.height * 10 + line.centrality + (if (match.value.contains("R$", true)) 30 else 0) +
                    (if (prefix.trimEnd().endsWith("por")) 100 else 0) - (if (prefix.trimEnd().endsWith("de")) 80 else 0) -
                    (if (Regex("(?i)(atacado|a partir|clube|cartão)").containsMatchIn(line.text)) 40 else 0)
                PriceCandidate(cents, if (weight) "kg" else "un", score)
            }.toList()
        }.sortedByDescending { it.score }.distinctBy { it.cents }.take(3)
    }
}
