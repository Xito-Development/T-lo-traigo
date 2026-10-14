package es.unocerobits.tlotraigo.data

import kotlinx.serialization.json.*
import es.unocerobits.tlotraigo.Locator
import java.net.URLEncoder

class Repository(val api: SupabaseApi) {
    private inline fun <reified T> parse(s: String): T = api.json.decodeFromString(s)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private val orderSel = "select=*,order_items(*,products(cold_chain),needs(cold_chain)),customer:profiles!orders_customer_id_fkey(full_name,phone)," +
        "driver:profiles!orders_driver_id_fkey(full_name,phone),order_reviews(order_id)"
    private val productCols = "id,supermarket_id,category_id,name,brand,unit,price,image_url,vat_rate,icon,active,ean,allergens,ingredients,age_restricted,cold_chain"

    // Catálogo
    suspend fun towns(): List<Town> = parse(api.rest("GET", "towns?active=eq.true&order=name"))
    suspend fun supermarkets(): List<Supermarket> = parse(api.rest("GET", "supermarkets?active=eq.true&select=id,name&order=id"))
    suspend fun categories(): List<Category> = parse(api.rest("GET", "categories?order=sort"))
    suspend fun fees(): FeeConfig = parse<List<FeeConfig>>(api.rest("GET", "fee_config?id=eq.1")).firstOrNull() ?: FeeConfig()

    suspend fun products(categoryId: Long?, query: String?): List<Product> {
        val sb = StringBuilder("products?select=$productCols&active=eq.true&order=name&limit=300")
        categoryId?.let { sb.append("&category_id=eq.").append(it) }
        if (!query.isNullOrBlank()) sb.append("&name=ilike.").append(enc("*${query.trim()}*").replace("+", "%20"))
        return parse(api.rest("GET", sb.toString()))
    }

    suspend fun productsByIds(ids: Collection<Long>): List<Product> =
        ids.filter { it > 0 }.let { pos -> if (pos.isEmpty()) emptyList()
        else parse(api.rest("GET", "products?select=$productCols&active=eq.true&id=in.(${pos.joinToString(",")})")) }

    // Perfil y familia
    suspend fun myProfile(): Profile? = parse<List<Profile>>(api.rest("GET", "profiles?id=eq.${api.uid}")).firstOrNull()

    suspend fun updateProfile(fullName: String, phone: String, address: String, townId: Long?, senior: Boolean? = null) {
        val body = buildJsonObject {
            put("full_name", fullName.trim()); put("phone", phone.trim()); put("address", address.trim())
            townId?.let { put("town_id", it) }
            senior?.let { put("senior_mode", it) }
        }
        api.rest("PATCH", "profiles?id=eq.${api.uid}", body.toString())
    }

    suspend fun linkedSeniors(): List<Profile> =
        parse<List<LinkRow>>(api.rest("GET", "family_links?familiar_id=eq.${api.uid}&select=senior:profiles!family_links_senior_id_fkey(*)")).map { it.senior }

    suspend fun linkSenior(code: String) { api.rpc("link_senior", buildJsonObject { put("p_code", code) }) }

    // Pedidos (cliente)
    suspend fun paymentMethods(townId: Long): List<String> =
        api.json.decodeFromString<List<String>?>(api.rpc("payment_methods_for_town", buildJsonObject { put("p_town", townId) })) ?: emptyList()

    /** Líneas de la cesta: productos (id > 0) o artículos del pedido guiado (id < 0, con need y detalle). */
    private fun itemsJson(lines: Map<Long, Int>) = buildJsonArray {
        lines.forEach { (id, q) ->
            val l = Locator.cart.lines.value[id]
            addJsonObject {
                if (l?.needId != null) { put("need_id", l.needId); put("detail", l.detail) } else put("product_id", id)
                put("qty", q)
            }
        }
    }

    /** Desglose de precios calculado en servidor (misma fórmula que al crear el pedido). */
    suspend fun quote(customer: String, supermarket: Long, lines: Map<Long, Int>, date: String?): Quote =
        parse(api.rpc("quote_order", buildJsonObject {
            put("p_customer", customer); put("p_supermarket", supermarket); put("p_items", itemsJson(lines)); put("p_date", date)
        }))

    suspend fun createOrder(customer: String, supermarket: Long, lines: Map<Long, Int>, method: String, address: String, notes: String?,
                            date: String? = null, slot: String? = null, adult: Boolean = false): Long {
        val args = buildJsonObject {
            put("p_customer", customer); put("p_supermarket", supermarket); put("p_payment", method)
            put("p_address", address); put("p_notes", notes?.takeIf { it.isNotBlank() })
            put("p_date", date); put("p_slot", slot); put("p_adult", adult)
            put("p_items", itemsJson(lines))
        }
        return api.rpc("create_order", args).trim().toLong()
    }

    suspend fun myOrders(): List<Order> =
        parse(api.rest("GET", "orders?$orderSel&or=(customer_id.eq.${api.uid},placed_by.eq.${api.uid})&order=created_at.desc&limit=50"))

    suspend fun order(id: Long): Order? = parse<List<Order>>(api.rest("GET", "orders?$orderSel&id=eq.$id")).firstOrNull()

    /** MOCKUP de pago: sustituir por webhook de Stripe/Redsys. */
    suspend fun mockMarkPaid(id: Long) { api.rpc("mock_mark_paid", buildJsonObject { put("p_order", id) }) }

    suspend fun setStatus(id: Long, status: String) {
        api.rpc("set_order_status", buildJsonObject { put("p_order", id); put("p_status", status) })
    }

    // Repartidor
    suspend fun pendingOrders(): List<Order> =
        parse(api.rest("GET", "orders?$orderSel&status=eq.pendiente&driver_id=is.null&order=created_at"))

    suspend fun myDeliveries(): List<Order> =
        parse(api.rest("GET", "orders?$orderSel&driver_id=eq.${api.uid}&order=created_at.desc&limit=50"))

    suspend fun acceptOrder(id: Long) { api.rpc("accept_order", buildJsonObject { put("p_order", id) }) }

    suspend fun driverSettings(): DriverSettings? =
        parse<List<DriverSettings>>(api.rest("GET", "driver_settings?driver_id=eq.${api.uid}")).firstOrNull()

    // --- Depuración (solo admin; el servidor lo valida)
    suspend fun debugSpawn(count: Int, townId: Long?, method: String?, ageHours: Int = 0): Int =
        api.rpc("debug_spawn_orders", buildJsonObject { put("p_count", count); put("p_town", townId); put("p_payment", method); put("p_age_hours", ageHours) }).trim().toInt()
    suspend fun debugRunPenalties(): Int = api.rpc("debug_run_penalties").trim().toInt()
    suspend fun debugAdvance(): Int = api.rpc("debug_advance_test_orders").trim().toInt()
    suspend fun debugClear(): Int = api.rpc("debug_clear_test_orders").trim().toInt()
    suspend fun drivers(): List<DriverRow> =
        parse(api.rest("GET", "driver_settings?select=driver_id,approved,available,profile:profiles!driver_settings_driver_id_fkey(full_name,phone)&order=approved"))
    suspend fun setDriverApproved(id: String, approved: Boolean) {
        api.rpc("admin_set_driver_approved", buildJsonObject { put("p_driver", id); put("p_approved", approved) })
    }
    suspend fun saveFees(pct: Double, min: Double) {
        api.rest("PATCH", "fee_config?id=eq.1", buildJsonObject { put("service_fee_pct", pct); put("min_service_fee", min) }.toString())
    }

    suspend fun saveDriverSettings(s: DriverSettings) {
        val body = buildJsonObject {
            put("available", s.available); put("accepts_cash", s.cash); put("accepts_bizum", s.bizum); put("accepts_card", s.card)
            putJsonArray("town_ids") { s.townIds.forEach { add(it) } }
        }
        api.rest("PATCH", "driver_settings?driver_id=eq.${api.uid}", body.toString())
    }

    // --- Repartidor: rechazo, ganancias, candidatura
    /** Devuelve true si el rechazo ha generado una penalización (pedido con más de 48 h). */
    suspend fun rejectOrder(id: Long): Boolean = api.rpc("reject_order", buildJsonObject { put("p_order", id) }).trim() == "true"
    suspend fun earnings(): Earnings = parse(api.rpc("driver_earnings"))
    suspend fun driverRating(id: String): Rating = parse(api.rpc("driver_rating", buildJsonObject { put("p_driver", id) }))
    suspend fun myApplication(): DriverApplication? =
        parse<List<DriverApplication>>(api.rest("GET", "driver_applications?driver_id=eq.${api.uid}")).firstOrNull()
    suspend fun submitApplication(vehicle: String, towns: List<Long>, availability: String, experience: String, message: String,
                                  adult: Boolean, selfEmployed: Boolean, insurance: Boolean, food: Boolean, terms: Boolean, geo: Boolean) {
        api.rpc("submit_application", buildJsonObject {
            put("p_vehicle", vehicle); putJsonArray("p_towns") { towns.forEach { add(it) } }
            put("p_availability", availability); put("p_experience", experience); put("p_message", message)
            put("p_adult", adult); put("p_self_employed", selfEmployed); put("p_insurance", insurance)
            put("p_food", food); put("p_terms", terms); put("p_geo", geo)
        })
    }
    suspend fun acceptTerms(version: String) { api.rpc("accept_terms", buildJsonObject { put("p_version", version) }) }
    suspend fun updateLocation(orderId: Long, lat: Double, lng: Double, acc: Float?) {
        api.rpc("update_driver_location", buildJsonObject { put("p_order", orderId); put("p_lat", lat); put("p_lng", lng); put("p_accuracy", acc) })
    }
    suspend fun driverLocation(orderId: Long): DriverLocation? =
        parse<List<DriverLocation>>(api.rest("GET", "driver_locations?order_id=eq.$orderId&select=lat,lng,updated_at")).firstOrNull()

    // --- Valoraciones
    suspend fun review(orderId: Long): Review? = parse<List<Review>>(api.rest("GET", "order_reviews?order_id=eq.$orderId")).firstOrNull()
    suspend fun submitReview(orderId: Long, driver: Int, delivery: Int, comment: String) {
        api.rpc("submit_review", buildJsonObject {
            put("p_order", orderId); put("p_driver_rating", driver); put("p_delivery_rating", delivery); put("p_comment", comment)
        })
    }
    suspend fun recentReviews(): List<Review> = parse(api.rest("GET", "order_reviews?order=created_at.desc&limit=100"))

    // --- Administración de catálogo (RLS: solo admin/superadmin)
    suspend fun adminProducts(query: String?): List<Product> {
        val q = if (query.isNullOrBlank()) "" else "&name=ilike." + enc("*${query.trim()}*").replace("+", "%20")
        return parse(api.rest("GET", "products?select=$productCols&order=active.desc,name&limit=500$q"))
    }
    suspend fun product(id: Long): Product? = parse<List<Product>>(api.rest("GET", "products?select=$productCols&id=eq.$id")).firstOrNull()

    suspend fun saveProduct(id: Long?, supermarket: Long, category: Long?, name: String, brand: String?, unit: String, price: Double,
                            vat: Double, icon: String?, image: String?, ean: String?, active: Boolean,
                            allergens: String? = null, ingredients: String? = null, ageRestricted: Boolean = false): Long {
        val body = buildJsonObject {
            put("supermarket_id", supermarket); put("category_id", category); put("name", name.trim())
            put("brand", brand?.trim()?.ifBlank { null }); put("unit", unit); put("price", price); put("vat_rate", vat)
            put("icon", icon); put("image_url", image); put("ean", ean?.trim()?.ifBlank { null }); put("active", active)
            put("allergens", allergens?.trim()?.ifBlank { null }); put("ingredients", ingredients?.trim()?.ifBlank { null })
            put("age_restricted", ageRestricted); put("source", "manual"); put("updated_at", java.time.OffsetDateTime.now().toString())
        }
        val res = if (id == null) api.rest("POST", "products?select=id", body.toString(), prefer = "return=representation")
                  else api.rest("PATCH", "products?id=eq.$id&select=id", body.toString(), prefer = "return=representation")
        return api.json.parseToJsonElement(res).jsonArray.first().jsonObject["id"]!!.jsonPrimitive.long
    }

    suspend fun uploadProductImage(bytes: ByteArray): String =
        api.upload("product-images", "p/${java.util.UUID.randomUUID()}.jpg", bytes, "image/jpeg")

    suspend fun saveFeeConfig(f: FeeConfig) {
        api.rest("PATCH", "fee_config?id=eq.1", buildJsonObject {
            put("service_fee_pct", f.servicePct); put("min_service_fee", f.minService); put("per_item_fee", f.perItem)
            put("free_items", f.freeItems); put("per_km_fee", f.perKm); put("express_fee", f.express)
            put("stale_penalty", f.stalePenalty); put("stale_hours", f.staleHours)
            put("platform_pct", f.platformPct); put("demo_payments", f.demoPayments); put("card_enabled", f.cardEnabled); put("payout_mode", f.payoutMode); put("cost_per_km", f.costPerKm); put("gestion_pct", f.gestionPct); put("gestion_min", f.gestionMin); put("price_margin_pct", f.marginPct)
        }.toString())
    }
    suspend fun addTown(name: String, cp: String?, fee: Double, km: Double) {
        api.rest("POST", "towns", buildJsonObject { put("name", name.trim()); put("postal_code", cp?.ifBlank { null }); put("delivery_fee", fee); put("distance_km", km) }.toString())
    }
    /** Los pueblos con pedidos no se pueden borrar: se ocultan (activo = no). */
    suspend fun deactivateTown(id: Long) { api.rest("PATCH", "towns?id=eq.$id", buildJsonObject { put("active", false) }.toString()) }
    suspend fun extraFees(): List<ExtraFee> = parse(api.rest("GET", "extra_fees?order=id"))
    suspend fun addExtraFee(name: String, kind: String, amount: Double) {
        api.rest("POST", "extra_fees", buildJsonObject { put("name", name.trim()); put("kind", kind); put("amount", amount) }.toString())
    }
    suspend fun deleteExtraFee(id: Long) { api.rest("DELETE", "extra_fees?id=eq.$id") }
    suspend fun saveAddress(a: Profile, fullName: String, phone: String) {
        api.rest("PATCH", "profiles?id=eq.${api.uid}", buildJsonObject {
            put("full_name", fullName.trim()); put("phone", phone.trim()); put("recipient_name", a.recipientName?.trim())
            put("street", a.street?.trim()); put("street_number", a.streetNumber?.trim()); put("floor_door", a.floorDoor?.trim()?.ifBlank { null })
            put("postal_code", a.postalCode?.trim()?.ifBlank { null }); put("province", a.province ?: "León"); put("directions", a.directions?.trim()?.ifBlank { null })
            put("town_id", a.townId); put("address", composeAddress(a))
        }.toString())
    }
    suspend fun saveTown(id: Long, fee: Double, km: Double) {
        api.rest("PATCH", "towns?id=eq.$id", buildJsonObject { put("delivery_fee", fee); put("distance_km", km) }.toString())
    }

    // --- Superadmin: repartidores, candidaturas y roles
    suspend fun applications(): List<DriverApplication> =
        parse(api.rest("GET", "driver_applications?select=*,driver:profiles!driver_applications_driver_id_fkey(full_name,phone)&order=status,created_at.desc"))
    suspend fun reviewApplication(id: Long, approve: Boolean, note: String?) {
        api.rpc("review_application", buildJsonObject { put("p_id", id); put("p_approve", approve); put("p_note", note) })
    }
    suspend fun adminUsers(): List<AdminUser> = parse(api.rpc("admin_users"))
    suspend fun setRole(email: String, role: String) {
        api.rpc("admin_set_role", buildJsonObject { put("p_email", api.normalizeEmail(email)); put("p_role", role) })
    }

    /** Elimina la cuenta (requisito de Google Play/App Store). Los pedidos se anonimizan. */
    suspend fun deleteMyAccount() { api.rpc("delete_my_account") }

    // --- Pedido guiado, ticket, pagos, reembolsos, facturas, reclamaciones y estadísticas
    suspend fun needs(): List<Need> = parse(api.rest("GET", "needs?active=eq.true&order=sort"))
    suspend fun cardCheckoutUrl(orderId: Long): String? =
        api.function("pagos", buildJsonObject { put("action", "checkout"); put("order_id", orderId) })["url"]?.jsonPrimitive?.contentOrNull
    // --- Entidades (modelo de suscripción)
    suspend fun organizations(): List<Organization> = parse(api.rest("GET", "organizations?order=name"))
    suspend fun organization(id: Long): Organization? = parse<List<Organization>>(api.rest("GET", "organizations?id=eq.$id")).firstOrNull()
    suspend fun createOrganization(name: String, kind: String, cName: String, cEmail: String, cPhone: String, towns: List<Long>): Pair<Long, String> {
        val r = api.json.parseToJsonElement(api.rpc("create_organization", buildJsonObject {
            put("p_name", name); put("p_kind", kind); put("p_contact_name", cName); put("p_contact_email", cEmail); put("p_contact_phone", cPhone)
            putJsonArray("p_towns") { towns.forEach { add(it) } } })).jsonObject
        return r["org_id"]!!.jsonPrimitive.long to r["code"]!!.jsonPrimitive.content
    }
    suspend fun updateOrganization(id: Long, fields: JsonObject) { api.rpc("update_organization", buildJsonObject { put("p_org", id); put("p", fields) }) }
    suspend fun orgOverview(id: Long): OrgOverview = parse(api.rpc("org_overview", buildJsonObject { put("p_org", id) }))
    suspend fun orgSaveTown(org: Long, id: Long?, name: String, cp: String?, fee: Double, km: Double, active: Boolean = true) {
        api.rpc("org_save_town", buildJsonObject { put("p_org", org); put("p_id", id); put("p_name", name); put("p_cp", cp); put("p_fee", fee); put("p_km", km); put("p_active", active) })
    }
    suspend fun orgSetDriver(driver: String, approved: Boolean, remove: Boolean = false) {
        api.rpc("org_set_driver", buildJsonObject { put("p_driver", driver); put("p_approved", approved); put("p_remove", remove) })
    }
    suspend fun createInvite(org: Long, role: String, maxUses: Int, days: Int, note: String?): String =
        api.json.parseToJsonElement(api.rpc("create_invite", buildJsonObject { put("p_org", org); put("p_role", role); put("p_max_uses", maxUses); put("p_days", days); put("p_note", note) })).jsonPrimitive.content
    suspend fun invites(org: Long): List<OrgInvite> = parse(api.rest("GET", "org_invites?org_id=eq.$org&order=created_at.desc&limit=30"))
    suspend fun redeemInvite(code: String): String {
        val r = api.json.parseToJsonElement(api.rpc("redeem_invite", buildJsonObject { put("p_code", code) })).jsonObject
        return "Te has unido a ${r["org"]?.jsonPrimitive?.content} como ${when (r["role"]?.jsonPrimitive?.content) { "gestor" -> "administrador/a"; "repartidor" -> "repartidor/a"; else -> "usuario/a" }}."
    }
    suspend fun orgReport(org: Long, month: java.time.LocalDate): OrgReport =
        parse(api.rpc("org_report", buildJsonObject { put("p_org", org); put("p_month", month.withDayOfMonth(1).toString()) }))
    suspend fun orgOrders(org: Long): List<Order> = parse(api.rest("GET", "orders?$orderSel&org_id=eq.$org&order=created_at.desc&limit=60"))

    // --- Chat, edición, reclamación y lista fácil (admin)
    suspend fun messages(orderId: Long): List<ChatMessage> = parse(api.rest("GET", "order_messages?order_id=eq.$orderId&order=id"))
    suspend fun sendMessage(orderId: Long, body: String) { api.rpc("send_message", buildJsonObject { put("p_order", orderId); put("p_body", body) }) }
    suspend fun unread(): List<Unread> = parse(api.rpc("unread_messages"))
    suspend fun markRead(orderId: Long) { runCatching { api.rpc("mark_messages_read", buildJsonObject { put("p_order", orderId) }) } }
    /** Sustituye los artículos del pedido. Cada línea: product_id o need_id (+ detalle) y cantidad. */
    suspend fun editOrder(orderId: Long, lines: List<Triple<OrderItem, Int, String?>>, notes: String?) {
        api.rpc("edit_order", buildJsonObject {
            put("p_order", orderId); put("p_notes", notes)
            putJsonArray("p_items") { lines.forEach { (it, q, det) -> addJsonObject {
                if (it.needId != null) { put("need_id", it.needId); put("detail", det ?: it.detail) } else put("product_id", it.productId)
                put("qty", q) } } }
        })
    }
    suspend fun claimOrder(orderId: Long, reason: String?) { api.rpc("claim_order", buildJsonObject { put("p_order", orderId); put("p_reason", reason) }) }
    suspend fun allNeeds(): List<Need> = parse(api.rest("GET", "needs?order=sort,name"))
    suspend fun saveNeed(id: Long?, n: Need) {
        val body = buildJsonObject {
            put("name", n.name.trim()); put("icon", n.icon); put("est_price", n.estPrice); put("unit_label", n.unitLabel); put("vat_rate", n.vatRate)
            put("cold_chain", n.coldChain); put("active", n.active); put("sort", n.sort)
            putJsonArray("options") { n.options.forEach { o -> addJsonObject { put("t", o.t); putJsonArray("v") { o.v.forEach { add(it) } } } } }
        }.toString()
        if (id == null) api.rest("POST", "needs", body) else api.rest("PATCH", "needs?id=eq.$id", body)
    }
    /** Si el artículo ya se usó en pedidos no se puede borrar: se oculta. */
    suspend fun deleteNeed(id: Long) {
        try { api.rest("DELETE", "needs?id=eq.$id") } catch (e: ApiException) { api.rest("PATCH", "needs?id=eq.$id", buildJsonObject { put("active", false) }.toString()) }
    }
    suspend fun giveTip(orderId: Long, amount: Double, method: String) {
        api.rpc("give_tip", buildJsonObject { put("p_order", orderId); put("p_amount", amount); put("p_method", method) })
    }
    suspend fun tipCheckoutUrl(orderId: Long): String? =
        api.function("pagos", buildJsonObject { put("action", "tip"); put("order_id", orderId) })["url"]?.jsonPrimitive?.contentOrNull
    suspend fun mockTipPaid(orderId: Long) { api.rpc("mock_tip_paid", buildJsonObject { put("p_order", orderId) }) }
    suspend fun confirmCash(orderId: Long) { api.rpc("confirm_cash", buildJsonObject { put("p_order", orderId) }) }
    suspend fun payout(orderId: Long) { runCatching { api.function("pagos", buildJsonObject { put("action", "payout"); put("order_id", orderId) }) } }
    suspend fun captureCard(orderId: Long) { runCatching { api.function("pagos", buildJsonObject { put("action", "capture"); put("order_id", orderId) }) } }
    suspend fun refundCard(orderId: Long) { runCatching { api.function("pagos", buildJsonObject { put("action", "refund"); put("order_id", orderId) }) } }
    suspend fun connectStripe(): String? = api.function("pagos", buildJsonObject { put("action", "connect") })["url"]?.jsonPrimitive?.contentOrNull
    suspend fun uploadTicket(orderId: Long, bytes: ByteArray): String {
        val path = "$orderId/${java.util.UUID.randomUUID()}.jpg"; api.upload("tickets", path, bytes, "image/jpeg"); return path
    }
    suspend fun setTicket(orderId: Long, path: String, total: Double, itemReals: Map<Long, Double> = emptyMap()): JsonObject =
        api.json.parseToJsonElement(api.rpc("set_ticket", buildJsonObject {
            put("p_order", orderId); put("p_path", path); put("p_total", total)
            if (itemReals.isNotEmpty()) putJsonArray("p_items") { itemReals.forEach { (id, r) -> addJsonObject { put("id", id); put("real", r) } } }
        })).jsonObject
    /** Vuelve a meter en la cesta un pedido anterior (productos y artículos de la lista fácil). */
    suspend fun repeatOrder(o: Order) {
        val prodQty = o.items.mapNotNull { i -> i.productId?.let { it to i.qty } }.toMap()
        Locator.cart.replaceWith(productsByIds(prodQty.keys).map { it to prodQty.getValue(it.id) })
        val needItems = o.items.filter { it.needId != null }
        if (needItems.isNotEmpty()) { val ns = needs().associateBy { it.id }; needItems.forEach { i -> ns[i.needId]?.let { Locator.cart.addNeed(it, i.detail.orEmpty(), i.qty) } } }
    }
    suspend fun ticketUrl(path: String) = api.signedUrl("tickets", path)
    suspend fun requestRefund(orderId: Long, reason: String, amount: Double?) {
        api.rpc("request_refund", buildJsonObject { put("p_order", orderId); put("p_reason", reason); put("p_amount", amount) })
    }
    suspend fun myRefunds(orderId: Long): List<RefundRequest> = parse(api.rest("GET", "refund_requests?order_id=eq.$orderId&order=created_at.desc"))
    suspend fun refundRequests(): List<RefundRequest> = parse(api.rest("GET", "refund_requests?order=status.desc,created_at.desc&limit=100"))
    suspend fun resolveRefund(id: Long, approve: Boolean, amount: Double?, note: String?) {
        val r = api.json.parseToJsonElement(api.rpc("resolve_refund", buildJsonObject { put("p_id", id); put("p_approve", approve); put("p_amount", amount); put("p_note", note) })).jsonObject
        r["order_id"]?.jsonPrimitive?.longOrNull?.let { if (approve) refundCard(it) }
    }
    suspend fun invoice(orderId: Long): Invoice? = parse<List<Invoice>>(api.rest("GET", "invoices?order_id=eq.$orderId")).firstOrNull()
    suspend fun company(): CompanyInfo? = parse<List<CompanyInfo>>(api.rest("GET", "company_info?id=eq.1")).firstOrNull()
    suspend fun saveCompany(c: CompanyInfo) { api.rest("PATCH", "company_info?id=eq.1", api.json.encodeToString(CompanyInfo.serializer(), c)) }
    suspend fun submitComplaint(orderId: Long?, name: String, contact: String, subject: String, body: String): String =
        api.json.parseToJsonElement(api.rpc("submit_complaint", buildJsonObject {
            put("p_order", orderId); put("p_name", name); put("p_contact", contact); put("p_subject", subject); put("p_body", body) })).jsonPrimitive.content
    suspend fun complaints(): List<Complaint> = parse(api.rest("GET", "complaints?order=created_at.desc&limit=100"))
    suspend fun answerComplaint(id: Long, text: String) { api.rpc("answer_complaint", buildJsonObject { put("p_id", id); put("p_response", text) }) }
    suspend fun analytics(days: Int): AnalyticsSummary = parse(api.rpc("analytics_summary", buildJsonObject { put("p_days", days) }))
    suspend fun track(events: kotlinx.serialization.json.JsonArray) { api.rpc("track", buildJsonObject { put("p_events", events) }) }
    suspend fun saveUsage(usage: String?, consent: Boolean?) {
        api.rest("PATCH", "profiles?id=eq.${api.uid}", buildJsonObject { usage?.let { put("usage", it) }; consent?.let { put("analytics_consent", it) } }.toString())
    }
}

/** Dirección completa para el repartidor: «Calle Mayor 12, 1º B — 24760 Villaquejida (León). Indicaciones». */
fun composeAddress(p: Profile, townName: String? = null): String = buildString {
    append(listOfNotNull(p.street?.trim()?.ifBlank { null }, p.streetNumber?.trim()?.ifBlank { null }).joinToString(" "))
    p.floorDoor?.trim()?.ifBlank { null }?.let { append(", ").append(it) }
    val loc = listOfNotNull(p.postalCode?.ifBlank { null }, townName).joinToString(" ")
    if (loc.isNotBlank()) append(" — ").append(loc)
    p.province?.ifBlank { null }?.let { append(" (").append(it).append(")") }
    p.directions?.trim()?.ifBlank { null }?.let { append(". ").append(it) }
}.trim().ifBlank { p.address.orEmpty() }
