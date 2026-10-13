package es.unocerobits.tlotraigo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class Town(
    val id: Long, val name: String,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("postal_code") val postalCode: String? = null,
    val active: Boolean = true,
)

@Serializable
data class ExtraFee(val id: Long, val name: String, val kind: String, val amount: Double, val active: Boolean = true)
@Serializable
data class QuoteExtra(val name: String, val amount: Double)

@Serializable
data class Supermarket(val id: Long, val name: String)

@Serializable
data class Category(val id: Long, val name: String, val icon: String = "", val sort: Int = 0, @SerialName("vat_rate") val vatRate: Double = 10.0)

@Serializable
data class Product(
    val id: Long,
    @SerialName("supermarket_id") val supermarketId: Long,
    @SerialName("category_id") val categoryId: Long? = null,
    val name: String,
    val brand: String? = null,
    val unit: String = "ud",
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("vat_rate") val vatRate: Double = 10.0,
    val icon: String? = null,
    val active: Boolean = true,
    val ean: String? = null,
    val allergens: String? = null,
    val ingredients: String? = null,
    @SerialName("age_restricted") val ageRestricted: Boolean = false,
    @SerialName("cold_chain") val coldChain: Boolean = false,
) {
    /** Precio sin IVA (los precios del súper son PVP con IVA incluido). */
    val basePrice get() = price / (1 + vatRate / 100)
    val vatAmount get() = price - basePrice
}

@Serializable
data class Profile(
    val id: String,
    val role: String = "cliente",
    @SerialName("full_name") val fullName: String = "",
    val phone: String? = null,
    val address: String? = null,
    @SerialName("town_id") val townId: Long? = null,
    @SerialName("senior_mode") val seniorMode: Boolean = true,
    @SerialName("link_code") val linkCode: String? = null,
    @SerialName("terms_version") val termsVersion: String? = null,
    val usage: String? = null,
    @SerialName("recipient_name") val recipientName: String? = null,
    val street: String? = null,
    @SerialName("street_number") val streetNumber: String? = null,
    @SerialName("floor_door") val floorDoor: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val province: String? = "León",
    val directions: String? = null,
    @SerialName("analytics_consent") val analyticsConsent: Boolean? = null,
) {
    val firstName get() = fullName.trim().substringBefore(' ').ifBlank { "" }
    val isDriver get() = role == "repartidor"
    val isAdmin get() = role == "admin" || role == "superadmin"
    val isSuper get() = role == "superadmin"
}

@Serializable
data class LinkRow(val senior: Profile)

@Serializable
data class DriverRow(
    @SerialName("driver_id") val driverId: String,
    val approved: Boolean = false,
    val available: Boolean = false,
    val profile: PersonLite? = null,
)

@Serializable
data class FeeConfig(
    @SerialName("service_fee_pct") val servicePct: Double = 10.0,
    @SerialName("min_service_fee") val minService: Double = 2.0,
    @SerialName("per_item_fee") val perItem: Double = 0.10,
    @SerialName("free_items") val freeItems: Int = 10,
    @SerialName("per_km_fee") val perKm: Double = 0.04,
    @SerialName("express_fee") val express: Double = 1.50,
    @SerialName("stale_penalty") val stalePenalty: Double = 1.50,
    @SerialName("stale_hours") val staleHours: Int = 48,
    @SerialName("gestion_pct") val gestionPct: Double = 5.0,
    @SerialName("gestion_min") val gestionMin: Double = 0.5,
    @SerialName("price_margin_pct") val marginPct: Double = 15.0,
    @SerialName("platform_pct") val platformPct: Double = 20.0,
    @SerialName("demo_payments") val demoPayments: Boolean = true,
    @SerialName("card_enabled") val cardEnabled: Boolean = false,
    @SerialName("payout_mode") val payoutMode: String = "gastos",
    @SerialName("cost_per_km") val costPerKm: Double = 0.26,
)

/** Desglose calculado en servidor (función quote_order). */
@Serializable
data class Quote(
    val subtotal: Double = 0.0,
    @SerialName("products_vat") val productsVat: Double = 0.0,
    val units: Int = 0,
    val service: Double = 0.0,
    @SerialName("service_pct") val servicePct: Double = 10.0,
    @SerialName("items_fee") val itemsFee: Double = 0.0,
    @SerialName("free_items") val freeItems: Int = 10,
    @SerialName("delivery_base") val deliveryBase: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("distance_fee") val distanceFee: Double = 0.0,
    @SerialName("express_fee") val expressFee: Double = 0.0,
    @SerialName("fees_vat") val feesVat: Double = 0.0,
    val gestion: Double = 0.0,
    @SerialName("gestion_pct") val gestionPct: Double = 5.0,
    @SerialName("max_total") val maxTotal: Double = 0.0,
    @SerialName("margin_pct") val marginPct: Double = 15.0,
    val extras: List<QuoteExtra> = emptyList(),
    @SerialName("payout_mode") val payoutMode: String = "gastos",
    @SerialName("extra_total") val extraTotal: Double = 0.0,
    val total: Double = 0.0,
    val town: String = "",
)

@Serializable
data class Review(
    @SerialName("order_id") val orderId: Long,
    @SerialName("driver_id") val driverId: String? = null,
    @SerialName("driver_rating") val driverRating: Int,
    @SerialName("delivery_rating") val deliveryRating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class DriverLocation(val lat: Double, val lng: Double, @SerialName("updated_at") val updatedAt: String)

@Serializable
data class Cold(@SerialName("cold_chain") val coldChain: Boolean = false)

@Serializable
data class NeedOption(val t: String, val v: List<String>)

/** Artículo genérico del pedido guiado (pan, leche…) con precio orientativo. */
@Serializable
data class Need(
    val id: Long, val name: String, val icon: String, @SerialName("est_price") val estPrice: Double,
    @SerialName("unit_label") val unitLabel: String = "ud", @SerialName("vat_rate") val vatRate: Double = 10.0,
    val options: List<NeedOption> = emptyList(), @SerialName("cold_chain") val coldChain: Boolean = false,
    val sort: Int = 0, val active: Boolean = true,
)

@Serializable
data class ChatMessage(val id: Long, @SerialName("order_id") val orderId: Long = 0, @SerialName("sender_role") val role: String,
    val body: String, @SerialName("created_at") val createdAt: String = "", @SerialName("read_at") val readAt: String? = null)
@Serializable
data class Unread(@SerialName("order_id") val orderId: Long, val id: Long, val body: String, val role: String)

@Serializable
data class Invoice(
    val number: String, @SerialName("issued_at") val issuedAt: String, val issuer: Map<String, String> = emptyMap(),
    @SerialName("customer_name") val customerName: String? = null, @SerialName("customer_address") val customerAddress: String? = null,
    val suplidos: Double, val base: Double, @SerialName("vat_rate") val vatRate: Double, val vat: Double, val total: Double,
    val hash: String = "", @SerialName("order_id") val orderId: Long = 0,
)

@Serializable
data class RefundRequest(val id: Long, @SerialName("order_id") val orderId: Long, val reason: String, val amount: Double? = null,
    val status: String, val note: String? = null, @SerialName("created_at") val createdAt: String)

@Serializable
data class Complaint(val id: Long, val number: String? = null, @SerialName("full_name") val fullName: String, val contact: String,
    val subject: String, val body: String, val status: String, val response: String? = null, @SerialName("created_at") val createdAt: String,
    @SerialName("order_id") val orderId: Long? = null)

@Serializable
data class CompanyInfo(val name: String, val nif: String, val address: String, val email: String, val series: String = "TLT")

@Serializable
data class DayCount(val day: String, val web: Int = 0, val app: Int = 0)
@Serializable
data class TopItem(val name: String, val source: String, val n: Int)
@Serializable
data class ErrorItem(val ts: String, val source: String, val name: String, val version: String? = null, val detail: kotlinx.serialization.json.JsonElement? = null)
@Serializable
data class AnalyticsSummary(val daily: List<DayCount> = emptyList(), val visitors: Map<String, Int> = emptyMap(),
    val top: List<TopItem> = emptyList(), val errors: List<ErrorItem> = emptyList())

@Serializable
data class Rating(val rating: Double? = null, val count: Int = 0)

@Serializable
data class Penalty(
    @SerialName("order_id") val orderId: Long, val amount: Double, val reason: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class Earnings(
    val from: String = "", val delivered: Int = 0, val gross: Double = 0.0, val penalties: Double = 0.0,
    val net: Double = 0.0, val tips: Double = 0.0, val rating: Double? = null, val reviews: Int = 0,
    @SerialName("penalty_list") val penaltyList: List<Penalty> = emptyList(),
)

@Serializable
data class DriverApplication(
    val id: Long,
    @SerialName("driver_id") val driverId: String,
    val vehicle: String,
    @SerialName("town_ids") val townIds: List<Long> = emptyList(),
    val availability: String? = null,
    val experience: String? = null,
    val message: String? = null,
    val status: String = "pendiente",
    @SerialName("review_note") val reviewNote: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val driver: PersonLite? = null,
)

@Serializable
data class AdminUser(
    val id: String, val email: String = "",
    @SerialName("full_name") val fullName: String = "",
    val role: String = "cliente", val phone: String? = null,
    val approved: Boolean? = null, val rating: Double? = null,
)

@Serializable
data class OrderItem(
    val id: Long,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("need_id") val needId: Long? = null,
    val detail: String? = null,
    @SerialName("real_total") val realTotal: Double? = null,
    val products: Cold? = null,
    val needs: Cold? = null,
    val name: String,
    @SerialName("unit_price") val unitPrice: Double,
    val qty: Int,
)

@Serializable
data class PersonLite(@SerialName("full_name") val fullName: String = "", val phone: String? = null)

@Serializable
data class Order(
    val id: Long,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("placed_by") val placedBy: String? = null,
    @SerialName("driver_id") val driverId: String? = null,
    @SerialName("town_id") val townId: Long,
    @SerialName("supermarket_id") val supermarketId: Long,
    @SerialName("delivery_address") val address: String,
    val status: String,
    val subtotal: Double,
    @SerialName("service_fee") val serviceFee: Double,
    @SerialName("delivery_fee") val deliveryFee: Double,
    val total: Double,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_status") val paymentStatus: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("order_items") val items: List<OrderItem> = emptyList(),
    val customer: PersonLite? = null,
    @SerialName("is_test") val isTest: Boolean = false,
    val driver: PersonLite? = null,
    @SerialName("express_fee") val expressFee: Double = 0.0,
    @SerialName("gestion_fee") val gestionFee: Double = 0.0,
    @SerialName("extra_fee") val extraFee: Double = 0.0,
    @SerialName("driver_payout") val driverPayout: Double? = null,
    @SerialName("tip_amount") val tipAmount: Double = 0.0,
    @SerialName("tip_status") val tipStatus: String = "ninguna",
    @SerialName("tip_method") val tipMethod: String? = null,
    @SerialName("claimed_at") val claimedAt: String? = null,
    @SerialName("claim_deadline") val claimDeadline: String? = null,
    @SerialName("max_total") val maxTotal: Double? = null,
    @SerialName("ticket_path") val ticketPath: String? = null,
    @SerialName("ticket_total") val ticketTotal: Double? = null,
    @SerialName("final_total") val finalTotal: Double? = null,
    @SerialName("ticket_over_max") val ticketOverMax: Boolean = false,
    @SerialName("amount_confirmed") val amountConfirmed: Boolean = false,
    @SerialName("refund_amount") val refundAmount: Double = 0.0,
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    @SerialName("scheduled_slot") val scheduledSlot: String? = null,
    @SerialName("order_reviews") val reviewRaw: JsonElement? = null,
) {
    val hasReview get() = reviewRaw is JsonObject || (reviewRaw is JsonArray && reviewRaw.isNotEmpty())
    val isFinal get() = status == "entregado" || status == "cancelado"
    val isPaid get() = paymentStatus == "pagado"
    val amount get() = finalTotal ?: total
    /** El repartidor subió el ticket y el cliente aún no ha pagado ni confirmado el efectivo. */
    val awaitingPayment get() = ticketTotal != null && !amountConfirmed && !isPaid && !isFinal
    val canCancel get() = status == "pendiente" || status == "aceptado"
    val hasCold get() = items.any { it.products?.coldChain == true || it.needs?.coldChain == true }
    /** Lo que gana el repartidor (sus tarifas menos la comisión de la plataforma). */
    val driverEarnings get() = driverPayout ?: ((serviceFee + deliveryFee + expressFee) * 0.8)
    /** Diferencia ticket − estimado (positivo: salió más caro). */
    val ticketDiff get() = ticketTotal?.let { it - subtotal }
}

@Serializable
data class DriverSettings(
    @SerialName("driver_id") val driverId: String,
    val available: Boolean = false,
    @SerialName("accepts_cash") val cash: Boolean = true,
    @SerialName("accepts_bizum") val bizum: Boolean = false,
    @SerialName("accepts_card") val card: Boolean = false,
    @SerialName("town_ids") val townIds: List<Long> = emptyList(),
    val approved: Boolean = false,
)

enum class OrderStatus(val db: String, val label: String, val clientText: String) {
    PENDIENTE("pendiente", "Recibido", "Esperando a que un repartidor lo acepte"),
    ACEPTADO("aceptado", "Aceptado", "Un repartidor se encarga de tu pedido"),
    COMPRANDO("comprando", "En el súper", "El repartidor está haciendo tu compra"),
    EN_CAMINO("en_camino", "En reparto", "Tu compra va hacia tu casa"),
    ENTREGADO("entregado", "Entregado", "Pedido entregado. ¡Que aproveche!"),
    CANCELADO("cancelado", "Cancelado", "Este pedido se canceló");

    companion object {
        fun of(db: String) = entries.firstOrNull { it.db == db } ?: PENDIENTE
        val flow = listOf(PENDIENTE, ACEPTADO, COMPRANDO, EN_CAMINO, ENTREGADO)
    }
}
