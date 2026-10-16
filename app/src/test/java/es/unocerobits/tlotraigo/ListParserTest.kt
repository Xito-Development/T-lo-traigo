package es.unocerobits.tlotraigo

import es.unocerobits.tlotraigo.data.Need
import es.unocerobits.tlotraigo.ui.screens.parseShoppingList
import org.junit.Assert.assertEquals
import org.junit.Test

class ListParserTest {
    private val needs = listOf("Pan", "Leche", "Huevos", "Fruta", "Agua", "Otra cosa").mapIndexed { i, n -> Need(i + 1L, n, "cesta", 1.0) }
    private fun parse(t: String) = parseShoppingList(t, needs).map { "${it.second}|${it.third}|${it.first.name}" }

    @Test fun dictadoConDudasYCorrecciones() = assertEquals(
        listOf("2|Barras de pan blanquinas|Pan", "5|Bricks de leche|Leche"),
        parse("2 barras de pan blanquinas ehhhm 6 bricks de no no 5 bricks de leche"))

    @Test fun numerosEnLetraSinComas() = assertEquals(
        listOf("2|Litros de leche|Leche", "6|Huevos|Huevos", "1|Pilas aa|Otra cosa"),
        parse("dos litros de leche seis huevos y pilas aa"))

    @Test fun perdonAnulaElArticuloAnterior() = assertEquals(
        listOf("3|Kilos de naranjas|Fruta"),
        parse("4 botellas de agua, perdón, 3 kilos de naranjas"))

    @Test fun comasYMuletillas() = assertEquals(
        listOf("1|Pan|Pan", "2|Paquetes de galletas|Otra cosa"),
        parse("bueno pues pan, mmm 2 paquetes de galletas por favor"))

    @Test fun hablaNatural() = assertEquals(
        listOf("2|Litros de leche|Leche", "1|Paquete de galletas|Otra cosa", "1|Manzanas|Fruta"),
        parse("quiero dos litros de leche, también un paquete de galletas y luego unas manzanas"))

    @Test fun parYDocena() = assertEquals(
        listOf("2|Barras de pan|Pan", "1|Docena de huevos|Huevos"),
        parse("ponme un par de barras de pan y una docena de huevos por favor"))

    @Test fun corregirCantidad() = assertEquals(
        listOf("3|Barras de pan|Pan"),
        parse("2 barras de pan, no, mejor 3"))

    @Test fun kiloYMedio() = assertEquals(
        listOf("1|Kilo y medio de naranjas|Fruta", "1|Agua|Agua"),
        parse("un kilo y medio de naranjas y agua nada más"))
}
