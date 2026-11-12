package br.com.carrinhocerto.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BudgetTest {
    @Test fun `orcamento aceita reais inteiros e centavos explicitos`() {
        assertEquals(20000L, parseBudget("200"))
        assertEquals(20000L, parseBudget("200,00"))
        assertEquals(20050L, parseBudget("200.50"))
        assertEquals(100000L, parseBudget("1.000"))
        assertEquals(100050L, parseBudget("1.000,50"))
    }
    @Test fun `desconta compras inclusive peso e devolve saldo ao remover`() {
        val budget = 20000L
        val items = listOf(1290L to BigDecimal("2"), 3990L to BigDecimal("0.750"))
        assertEquals(14427L, remainingBudget(budget, total(items)))
        assertEquals(17007L, remainingBudget(budget, total(items.drop(1))))
        assertEquals(20000L, remainingBudget(budget, total(emptyList())))
    }
    @Test fun `mostra zero no limite e valor negativo ao ultrapassar`() {
        assertEquals(0L, remainingBudget(10000, 10000))
        assertEquals(-1290L, remainingBudget(10000, 11290))
    }
}
