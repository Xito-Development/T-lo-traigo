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
)

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
data class Rating(val rating: Double? = null, val count: Int = 0)

@Serializable
data class Penalty(
    @SerialName("order_id") val orderId: Long, val amount: Double, val reason: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class Earnings(
    val from: String = "", val delivered: Int = 0, val gross: Double = 0.0, val penalties: Double = 0.0,
    val net: Double = 0.0, val rating: Double? = null, val reviews: Int = 0,
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
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    @SerialName("scheduled_slot") val scheduledSlot: String? = null,
    @SerialName("order_reviews") val reviewRaw: JsonElement? = null,
) {
    val hasReview get() = reviewRaw is JsonObject || (reviewRaw is JsonArray && reviewRaw.isNotEmpty())
    val isFinal get() = status == "entregado" || status == "cancelado"
    val isPaid get() = paymentStatus == "pagado"
    val driverEarnings get() = serviceFee + deliveryFee + expressFee
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
    COMPRANDO("comprando", "Comprando", "Están haciendo tu compra en el súper"),
    EN_CAMINO("en_camino", "En camino", "Tu compra va hacia tu casa"),
    ENTREGADO("entregado", "Entregado", "Pedido entregado. ¡Que aproveche!"),
    CANCELADO("cancelado", "Cancelado", "Este pedido se canceló");

    companion object {
        fun of(db: String) = entries.firstOrNull { it.db == db } ?: PENDIENTE
        val flow = listOf(PENDIENTE, ACEPTADO, COMPRANDO, EN_CAMINO, ENTREGADO)
    }
}
