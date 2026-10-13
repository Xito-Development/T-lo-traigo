// Pasarela de pago T lo traigo (Stripe). Acciones: checkout, capture, refund, connect.
// Secretos necesarios en Supabase → Edge Functions → Secrets: STRIPE_SECRET_KEY (sk_live_… o sk_test_…)
const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const SK = Deno.env.get("STRIPE_SECRET_KEY") ?? "";
const WEB = "https://tlotraigo.unocerobits.com";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type" };
const j = (b: unknown, s = 200) => new Response(JSON.stringify(b), { status: s, headers: { ...cors, "Content-Type": "application/json" } });

async function db(path: string, init: RequestInit = {}) {
  const r = await fetch(`${SB}/rest/v1/${path}`, { ...init, headers: { apikey: SR, Authorization: `Bearer ${SR}`, "Content-Type": "application/json", Prefer: "return=representation", ...(init.headers ?? {}) } });
  if (!r.ok) throw new Error(`BD ${r.status}: ${await r.text()}`);
  const t = await r.text(); return t ? JSON.parse(t) : null;
}
async function stripe(path: string, params: Record<string, string> = {}, method = "POST") {
  const r = await fetch(`https://api.stripe.com/v1/${path}`, { method, headers: { Authorization: `Bearer ${SK}`, "Content-Type": "application/x-www-form-urlencoded" },
    body: method === "GET" ? undefined : new URLSearchParams(params) });
  const b = await r.json(); if (!r.ok) throw new Error(b?.error?.message ?? "Error de Stripe"); return b;
}
const cents = (e: number) => String(Math.round(e * 100));

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: cors });
  try {
    if (!SK) return j({ error: "La pasarela de pago aún no está configurada. Paga en efectivo o inténtalo más tarde." }, 503);
    const u = await fetch(`${SB}/auth/v1/user`, { headers: { apikey: SR, Authorization: req.headers.get("Authorization") ?? "" } });
    if (!u.ok) return j({ error: "Sesión no válida" }, 401);
    const uid = (await u.json()).id as string;
    const { action, order_id } = await req.json();
    const prof = (await db(`profiles?id=eq.${uid}&select=role`))[0];
    const admin = prof && ["admin", "superadmin"].includes(prof.role);

    if (action === "connect") {   // Alta del repartidor en Stripe Connect (Express)
      let ds = (await db(`driver_settings?driver_id=eq.${uid}&select=stripe_account_id`))[0];
      if (!ds) return j({ error: "Solo repartidores" }, 403);
      let acc = ds.stripe_account_id;
      if (!acc) {
        acc = (await stripe("accounts", { type: "express", country: "ES", "capabilities[transfers][requested]": "true", "business_type": "individual", "metadata[user]": uid })).id;
        await db(`driver_settings?driver_id=eq.${uid}`, { method: "PATCH", body: JSON.stringify({ stripe_account_id: acc }) });
      }
      const a = await stripe(`accounts/${acc}`, {}, "GET");
      if (a.payouts_enabled) { await db(`driver_settings?driver_id=eq.${uid}`, { method: "PATCH", body: JSON.stringify({ stripe_ready: true }) }); return j({ ready: true }); }
      const link = await stripe("account_links", { account: acc, type: "account_onboarding", refresh_url: `${WEB}/stripe-volver.html`, return_url: `${WEB}/stripe-volver.html` });
      return j({ url: link.url });
    }

    const o = (await db(`orders?id=eq.${Number(order_id)}&select=*`))[0];
    if (!o) return j({ error: "Pedido no encontrado" }, 404);
    const links = await db(`family_links?familiar_id=eq.${uid}&senior_id=eq.${o.customer_id}&select=senior_id`);
    const isCustomer = o.placed_by === uid || o.customer_id === uid || links.length > 0;

    if (action === "checkout") {   // Autoriza el importe máximo; se cobra lo real al subir el ticket
      if (!isCustomer) return j({ error: "No autorizado" }, 403);
      if (o.payment_method !== "tarjeta" || o.payment_status !== "pendiente") return j({ error: "Este pedido no necesita pago con tarjeta" }, 400);
      const s = await stripe("checkout/sessions", {
        mode: "payment", locale: "es", "payment_method_types[0]": "card",
        "line_items[0][quantity]": "1", "line_items[0][price_data][currency]": "eur",
        "line_items[0][price_data][unit_amount]": cents(Number(o.max_total ?? o.total)),
        "line_items[0][price_data][product_data][name]": `Pedido nº ${o.id} · T lo traigo (importe máximo; se cobra lo real del ticket)`,
        "payment_intent_data[capture_method]": "manual", "payment_intent_data[metadata][order_id]": String(o.id),
        "payment_intent_data[description]": `T lo traigo pedido ${o.id}`, "metadata[order_id]": String(o.id),
        success_url: `${WEB}/pago-ok.html?pedido=${o.id}`, cancel_url: `${WEB}/pago-cancelado.html?pedido=${o.id}`,
      });
      await db(`orders?id=eq.${o.id}`, { method: "PATCH", body: JSON.stringify({ stripe_session_id: s.id }) });
      return j({ url: s.url });
    }

    if (action === "capture") {   // Cobro del importe real (ticket + tarifas), con tope en lo autorizado
      if (o.driver_id !== uid && !admin) return j({ error: "No autorizado" }, 403);
      if (o.payment_status !== "autorizado" || !o.stripe_payment_intent) return j({ skipped: true });
      if (o.final_total == null) return j({ error: "Falta el ticket" }, 400);
      const amount = Math.min(Number(o.final_total), Number(o.max_total ?? o.total));
      const pi = await stripe(`payment_intents/${o.stripe_payment_intent}/capture`, { amount_to_capture: cents(amount) });
      await db(`orders?id=eq.${o.id}`, { method: "PATCH", body: JSON.stringify({ payment_status: "pagado" }) });
      const ds = o.driver_id && (await db(`driver_settings?driver_id=eq.${o.driver_id}&select=stripe_account_id,stripe_ready`))[0];
      if (ds?.stripe_ready && ds.stripe_account_id && pi.latest_charge) {   // reparto del pago al repartidor (Connect)
        const share = Number(o.service_fee) + Number(o.delivery_fee) + Number(o.express_fee);
        await stripe("transfers", { amount: cents(share), currency: "eur", destination: ds.stripe_account_id, source_transaction: pi.latest_charge, transfer_group: `pedido_${o.id}` });
      }
      return j({ captured: amount });
    }

    if (action === "refund") {   // Devuelve lo aprobado (refund_amount) que aún no se haya devuelto
      if (!isCustomer && !admin) return j({ error: "No autorizado" }, 403);
      const due = Number(o.refund_amount) - Number(o.stripe_refunded);
      if (o.payment_method !== "tarjeta" || !o.stripe_payment_intent || due <= 0) return j({ skipped: true });
      if (o.payment_status === "autorizado") {   // aún no cobrado: se libera o se cobra solo lo no reembolsable
        const keep = Number(o.max_total ?? o.total) - Number(o.refund_amount);
        if (keep <= 0.009) await stripe(`payment_intents/${o.stripe_payment_intent}/cancel`);
        else await stripe(`payment_intents/${o.stripe_payment_intent}/capture`, { amount_to_capture: cents(Math.min(keep, Number(o.service_fee) + Number(o.gestion_fee))) });
      } else {
        await stripe("refunds", { payment_intent: o.stripe_payment_intent, amount: cents(due), reason: "requested_by_customer" });
      }
      const total = Number(o.final_total ?? o.total);
      await db(`orders?id=eq.${o.id}`, { method: "PATCH", body: JSON.stringify({ stripe_refunded: Number(o.refund_amount), refunded_at: new Date().toISOString(),
        payment_status: Number(o.refund_amount) >= total - 0.009 ? "reembolsado" : "reembolso_parcial" }) });
      return j({ refunded: due });
    }
    return j({ error: "Acción no válida" }, 400);
  } catch (e) { return j({ error: (e as Error).message }, 500); }
});
