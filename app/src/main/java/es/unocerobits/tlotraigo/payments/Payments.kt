package es.unocerobits.tlotraigo.payments

import kotlinx.coroutines.delay

/** Métodos de pago modulares. Los activa cada repartidor; aquí solo se simulan (mockup). */
enum class PayMethod(val db: String, val label: String, val hint: String) {
    CASH("efectivo", "Efectivo", "Pagas al repartidor al recibir la compra"),
    BIZUM("bizum", "Bizum", "Pago desde el móvil (simulado)"),
    CARD("tarjeta", "Tarjeta", "Pago seguro con Stripe (simulado)");

    companion object { fun of(db: String) = entries.firstOrNull { it.db == db } ?: CASH }
}

sealed interface PayResult {
    data object Deferred : PayResult            // se cobra en la entrega
    data object Paid : PayResult
    data class Failed(val reason: String) : PayResult
}

interface PaymentProvider {
    val method: PayMethod
    suspend fun pay(orderId: Long, amountEur: Double): PayResult
}

class CashProvider : PaymentProvider {
    override val method = PayMethod.CASH
    override suspend fun pay(orderId: Long, amountEur: Double) = PayResult.Deferred
}

/** TODO producción: solicitud Bizum vía TPV/Redsys desde una Edge Function. */
class BizumMockProvider : PaymentProvider {
    override val method = PayMethod.BIZUM
    override suspend fun pay(orderId: Long, amountEur: Double): PayResult { delay(1600); return PayResult.Paid }
}

/** TODO producción: Stripe PaymentSheet + Edge Function que cree el PaymentIntent y webhook que marque el pedido. */
class StripeMockProvider : PaymentProvider {
    override val method = PayMethod.CARD
    override suspend fun pay(orderId: Long, amountEur: Double): PayResult { delay(2000); return PayResult.Paid }
}

object Payments {
    private val providers = listOf(CashProvider(), BizumMockProvider(), StripeMockProvider()).associateBy { it.method }
    fun provider(m: PayMethod): PaymentProvider = providers.getValue(m)
}
