package es.unocerobits.tlotraigo

import es.unocerobits.tlotraigo.ui.screens.validTaxId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxIdTest {
    @Test fun validos() = listOf("12345678Z", "X1234567L", "B12345674", "A58818501", "Q2826000H").forEach { assertTrue(it, validTaxId(it)) }
    @Test fun invalidos() = listOf("12345678A", "B12345675", "", "ABC").forEach { assertFalse(it, validTaxId(it)) }
}
