package br.com.carrinhocerto.domain

import org.junit.Assert.*
import org.junit.Test

class PriceRankingTest {
    @Test fun `destaque visual vence texto pequeno e limita alternativas`() {
        val prices = PriceTagParser().parse(listOf(TagLine("R$ 1,00", 10), TagLine("R$ 9,90", 50), TagLine("R$ 2,00", 20), TagLine("R$ 3,00", 30)))
        assertEquals(990L, prices.first().cents)
        assertEquals(3, prices.size)
    }
}
