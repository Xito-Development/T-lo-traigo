// Webhook de Stripe (sin JWT; se valida la firma). Secreto: STRIPE_WEBHOOK_SECRET (whsec_…)
const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WH = Deno.env.get("STRIPE_WEBHOOK_SECRET") ?? "";
const SK = Deno.env.get("STRIPE_SECRET_KEY") ?? "";
const get = async (path: string) => (await fetch(`${SB}/rest/v1/${path}`, { headers: { apikey: SR, Authorization: `Bearer ${SR}` } })).json();
const stripe = async (path: string, params?: Record<string, string>) => (await fetch(`https://api.stripe.com/v1/${path}`, {
  method: params ? "POST" : "GET", headers: { Authorization: `Bearer ${SK}`, "Content-Type": "application/x-www-form-urlencoded" },
  body: params ? new URLSearchParams(params) : undefined })).json();
const db = (path: string, body: unknown) => fetch(`${SB}/rest/v1/${path}`, { method: "PATCH",
  headers: { apikey: SR, Authorization: `Bearer ${SR}`, "Content-Type": "application/json" }, body: JSON.stringify(body) });

async function valid(payload: string, header: string) {
  const parts = Object.fromEntries(header.split(",").map((p) => p.split("=") as [string, string]));
  if (!parts.t || !parts.v1 || Math.abs(Date.now() / 1000 - Number(parts.t)) > 300) return false;
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(WH), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const sig = new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`${parts.t}.${payload}`)));
  const hex = [...sig].map((b) => b.toString(16).padStart(2, "0")).join("");
  return header.split(",").some((p) => p === `v1=${hex}`);
}

Deno.serve(async (req) => {
  const payload = await req.text();
  if (!WH || !(await valid(payload, req.headers.get("stripe-signature") ?? ""))) return new Response("firma no válida", { status: 400 });
  const ev = JSON.parse(payload); const o = ev.data?.object ?? {};
  if (ev.type === "checkout.session.completed" && o.metadata?.order_id && o.metadata?.kind === "tip") {
    const id = Number(o.metadata.order_id);
    await db(`orders?id=eq.${id}`, { tip_status: "pagada", tip_payment_intent: o.payment_intent });
    const ord = (await get(`orders?id=eq.${id}&select=driver_id,tip_amount`))[0];   // propina íntegra al repartidor (Connect)
    const ds = ord?.driver_id && (await get(`driver_settings?driver_id=eq.${ord.driver_id}&select=stripe_account_id,stripe_ready`))[0];
    if (SK && ds?.stripe_ready && ds.stripe_account_id) {
      const pi = await stripe(`payment_intents/${o.payment_intent}`);
      if (pi.latest_charge) await stripe("transfers", { amount: String(Math.round(Number(ord.tip_amount) * 100)), currency: "eur",
        destination: ds.stripe_account_id, source_transaction: pi.latest_charge, transfer_group: `propina_${id}` });
    }
  } else if (ev.type === "checkout.session.completed" && o.metadata?.order_id)
    await db(`orders?id=eq.${Number(o.metadata.order_id)}`, { payment_status: "pagado", amount_confirmed: true, stripe_payment_intent: o.payment_intent });
  if (ev.type === "checkout.session.expired" && o.metadata?.order_id)
    await db(`orders?id=eq.${Number(o.metadata.order_id)}&payment_status=eq.pendiente`, { stripe_session_id: null });
  if (ev.type === "account.updated" && o.id)
    await db(`driver_settings?stripe_account_id=eq.${o.id}`, { stripe_ready: !!o.payouts_enabled });
  return new Response("ok");
});
