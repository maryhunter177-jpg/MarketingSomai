package br.com.carrinhocerto.domain

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PriceTagParserTest(private val label: String, private val expected: Long?, private val unit: String) {
    @Test fun `extrai preco da etiqueta`() {
        val result = PriceTagParser().parse(label).firstOrNull()
        assertEquals(label, expected, result?.cents)
        if (expected != null) assertEquals(label, unit, result?.unit)
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}") fun cases(): List<Array<Any?>> = listOf(
            arrayOf("R$ 12,90", 1290L, "un"), arrayOf("12,90", 1290L, "un"),
            arrayOf("R$12,90", 1290L, "un"), arrayOf("12 90", 1290L, "un"),
            arrayOf("1.299,00", 129900L, "un"), arrayOf("ARROZ 5kg\nR$ 22,99", 2299L, "un"),
            arrayOf("500g\nR$ 7,49", 749L, "un"), arrayOf("7891234567890", null, "un"),
            arrayOf("VALIDADE 12/09/2026", null, "un"), arrayOf("50% OFF", null, "un"),
            arrayOf("3x\nR$ 4,99", 499L, "un"), arrayOf("leve 3 pague 2\nR$ 8,90", 890L, "un"),
            arrayOf("a partir de 6 un R$ 9,90\nR$ 11,90", 1190L, "un"),
            arrayOf("de R$ 19,90\npor R$ 14,90", 1490L, "un"),
            arrayOf("de 19,90 por 14,90", 1490L, "un"), arrayOf("CARNE /kg\nR$ 39,90", 3990L, "kg"),
            arrayOf("MAÇÃ kg 8,99", 899L, "kg"), arrayOf("QUEIJO 100g\nR$ 5,99", 5990L, "kg"),
            arrayOf("OFERTA!!!\nR$ 0,99", 99L, "un"), arrayOf("12.90", 1290L, "un"),
            arrayOf("LOTE 1234567890123\nR$ 2,39", 239L, "un"), arrayOf("", null, "un"),
            arrayOf("SEM PREÇO", null, "un"), arrayOf("12,90% OFF", null, "un"),
            arrayOf("500,00g", null, "un"), arrayOf("CLUBE R$ 7,99\nR$ 9,99", 999L, "un"),
            arrayOf("ATACADO R$ 2,50\nVAREJO R$ 3,00", 300L, "un"),
            arrayOf("R$ 99 90", 9990L, "un"), arrayOf("por 1.099,99 /kg", 109999L, "kg"),
            arrayOf("10,00ml", null, "un"), arrayOf("10,00/12/2026", null, "un")
        )
    }
}
