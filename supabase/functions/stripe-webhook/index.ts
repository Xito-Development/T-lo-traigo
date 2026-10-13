// Webhook de Stripe (sin JWT; se valida la firma). Secreto: STRIPE_WEBHOOK_SECRET (whsec_…)
const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WH = Deno.env.get("STRIPE_WEBHOOK_SECRET") ?? "";
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
  if (ev.type === "checkout.session.completed" && o.metadata?.order_id)
    await db(`orders?id=eq.${Number(o.metadata.order_id)}`, { payment_status: "autorizado", stripe_payment_intent: o.payment_intent });
  if (ev.type === "checkout.session.expired" && o.metadata?.order_id)
    await db(`orders?id=eq.${Number(o.metadata.order_id)}&payment_status=eq.pendiente`, { payment_status: "fallido" });
  if (ev.type === "account.updated" && o.id)
    await db(`driver_settings?stripe_account_id=eq.${o.id}`, { stripe_ready: !!o.payouts_enabled });
  return new Response("ok");
});
