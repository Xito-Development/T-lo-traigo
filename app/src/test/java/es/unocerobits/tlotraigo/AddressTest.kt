package es.unocerobits.tlotraigo

import es.unocerobits.tlotraigo.data.Profile
import es.unocerobits.tlotraigo.data.composeAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressTest {
    private fun p(street: String?, num: String?, floor: String? = null, cp: String? = null, dir: String? = null) =
        Profile(id = "x", fullName = "Rosa", street = street, streetNumber = num, floorDoor = floor, postalCode = cp, directions = dir)

    @Test fun completa() = assertEquals("Calle Mayor 1, 2º B, 24235 Villaquejida (León). Puerta verde",
        composeAddress(p("calle Mayor", "1", "2º B", "24235", "Puerta verde"), "Villaquejida"))
    @Test fun sinCodigoPostal() = assertEquals("Calle Real 3, Villafer (León)", composeAddress(p("Calle Real", "3"), "Villafer"))
    @Test fun incluyeSiempreElPueblo() = assertEquals("Plaza 5, 24236 Villafer (León)", composeAddress(p("Plaza", "5", cp = "24236"), "Villafer"))
}
