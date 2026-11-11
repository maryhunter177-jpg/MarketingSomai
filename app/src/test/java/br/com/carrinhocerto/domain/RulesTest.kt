package br.com.carrinhocerto.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class RulesTest {
    @Test fun `total exato com unidades e peso`() {
        assertEquals(4878L, total(listOf(1290L to BigDecimal("3"), 1344L to BigDecimal("0.750"))))
        assertEquals(101L, subtotal(201, BigDecimal("0.5")))
        assertEquals(0L, total(emptyList()))
    }
    @Test fun `incrementa decrementa e remove em zero`() {
        assertEquals(BigDecimal("3"), changedQuantity(BigDecimal("2"), BigDecimal.ONE))
        assertEquals(BigDecimal.ONE, changedQuantity(BigDecimal("2"), BigDecimal.ONE.negate()))
        assertNull(changedQuantity(BigDecimal.ONE, BigDecimal.ONE.negate()))
        assertNull(changedQuantity(BigDecimal("0.050"), BigDecimal("-0.100")))
        assertEquals(BigDecimal("0.850"), changedQuantity(BigDecimal("0.750"), BigDecimal("0.100")))
    }
    @Test fun `debounce por codigo por dois segundos`() {
        val gate = ScanDebouncer()
        assertTrue(gate.accept("123", 0)); assertFalse(gate.accept("123", 1999))
        assertTrue(gate.accept("456", 1999)); assertTrue(gate.accept("123", 2000))
        assertFalse(gate.accept("123", 2001))
    }
    @Test fun `precos digitados nunca usam ponto flutuante`() {
        listOf("12,90", "12.90", "1290", "R$ 12,90").forEach { assertEquals(it, 1290L, parsePrice(it)) }
        assertEquals(129900L, parsePrice("1.299,00"))
        assertEquals(1L, parsePrice("1"))
        listOf("", "-1", "0", "12,9", "1e3", "NaN", "99999999999999999999").forEach { assertNull(it, parsePrice(it)) }
    }
    @Test fun `quantidade exige inteiros para unidade e tres casas para kg`() {
        assertEquals(BigDecimal("0.750"), quantity("0,750", "kg"))
        assertNull(quantity("0,75", "un")); assertNull(quantity("0", "un"))
        assertNull(quantity("10000", "kg")); assertNull(quantity("0,0001", "kg"))
    }
}
