// Correos de la app (Brevo): «pedido recibido» y «pedido entregado + factura/justificante».
// Lo llama la base de datos (pg_net) con la cabecera x-tlt-secret. Secreto necesario: BREVO_API_KEY (Brevo → SMTP y API → Claves API).
const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const ENV_BREVO = Deno.env.get("BREVO_API_KEY") ?? "";   // o la clave guardada desde la app (app_secrets.brevo_api_key)
const FROM = { name: "T Lo Traigo UNOCEROBITS", email: "tlotraigo@unocerobits.com" };
const LOGO = "https://raw.githubusercontent.com/Xito-Development/T-lo-traigo/main/web/t-lo-traigo/email/logo.png";
const H = { apikey: SR, Authorization: `Bearer ${SR}` };
const get = async (p: string) => (await fetch(`${SB}/rest/v1/${p}`, { headers: H })).json();
const eur = (n: number) => (Number(n) || 0).toLocaleString("es-ES", { style: "currency", currency: "EUR" });
const esc = (s: unknown) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]!));

async function emailOf(uid: string | null) {
  if (!uid) return null;
  const r = await fetch(`${SB}/auth/v1/admin/users/${uid}`, { headers: H });
  if (!r.ok) return null; const u = await r.json();
  return u.email && !String(u.email).endsWith("@tlotraigo.app") ? u.email as string : null;   // las cuentas cortas internas no tienen buzón
}

function layout(title: string, intro: string, body: string) {
  return `<!doctype html><html lang="es"><body style="margin:0;background:#F3F7F4;font-family:Verdana,Arial,sans-serif;color:#1C2624">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:24px 12px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:580px;background:#fff;border-radius:20px;overflow:hidden">
<tr><td align="center" style="background:#2E6B57;padding:26px 24px 20px">
<img src="${LOGO}" width="64" height="64" alt="T lo traigo" style="display:block;border:3px solid #FFF8EC;border-radius:50%;margin:0 auto 10px">
<p style="margin:0;font-family:'Trebuchet MS',Verdana,sans-serif;font-size:32px;font-weight:bold;color:#FFF8EC">T lo traigo</p>
<div style="width:140px;height:5px;margin:6px auto 0;background:#F2B544;border-radius:0 0 80px 80px"></div></td></tr>
<tr><td style="padding:28px 30px 6px"><h1 style="margin:0 0 10px;font-size:24px;color:#1F4D3F">${title}</h1><p style="margin:0 0 16px;font-size:16px;line-height:1.55">${intro}</p></td></tr>
<tr><td style="padding:0 30px 24px">${body}</td></tr>
<tr><td align="center" style="background:#17382E;padding:18px;font-size:12.5px;line-height:1.7;color:#B9C7C2">
¿Dudas? <a href="mailto:support@tlotraigo.unocerobits.com" style="color:#F2B544">support@tlotraigo.unocerobits.com</a><br>
<a href="https://tlotraigo.unocerobits.com" style="color:#fff">tlotraigo.unocerobits.com</a> · Puedes desactivar estos correos en Ajustes de la app.</td></tr>
</table></td></tr></table></body></html>`;
}
function itemsTable(items: any[], real: boolean) {
  const rows = items.map((i) => {
    const name = i.unavailable ? `<s>${esc(i.name)}</s> <span style="color:#B23A48">(no había)</span>`
      : i.substitute ? `${esc(i.name)} → <b>${esc(i.substitute)}</b>` : esc(i.name) + (i.detail ? `<br><span style="color:#5D6B67;font-size:13px">${esc(i.detail)}</span>` : "");
    const amount = i.unavailable ? 0 : real && i.real_total != null ? i.real_total : i.unit_price * i.qty;
    return `<tr><td style="padding:8px 0;border-bottom:1px solid #E3ECE7;font-size:14px"><b>${i.qty} ×</b> ${name}</td><td align="right" style="padding:8px 0;border-bottom:1px solid #E3ECE7;font-size:14px;white-space:nowrap">${eur(amount)}</td></tr>`;
  }).join("");
  return `<table role="presentation" width="100%" cellpadding="0" cellspacing="0">${rows}</table>`;
}
const line = (a: string, b: string, strong = false) => `<tr><td style="padding:4px 0;font-size:${strong ? 17 : 14}px;${strong ? "font-weight:bold;color:#1F4D3F" : ""}">${a}</td><td align="right" style="padding:4px 0;font-size:${strong ? 17 : 14}px;${strong ? "font-weight:bold;color:#1F4D3F" : ""}">${b}</td></tr>`;

Deno.serve(async (req) => {
  try {
    const secret = (await get("app_secrets?key=eq.correos&select=value"))[0]?.value;
    if (!secret || req.headers.get("x-tlt-secret") !== secret) return new Response("no autorizado", { status: 401 });
    const BREVO = ENV_BREVO || (await get("app_secrets?key=eq.brevo_api_key&select=value"))[0]?.value || "";
    if (!BREVO) return new Response("correo no configurado: falta la clave de Brevo");
    const { order_id, kind, user_id } = await req.json();
    const send = async (to: { email: string; name: string }[], subject: string, html: string) => {
      const r = await fetch("https://api.brevo.com/v3/smtp/email", { method: "POST", headers: { "api-key": BREVO, "Content-Type": "application/json", accept: "application/json" },
        body: JSON.stringify({ sender: FROM, to, subject, htmlContent: html, tags: ["app", kind] }) });
      return new Response(r.ok ? "enviado a " + to.map((t) => t.email).join(", ") : "error de Brevo " + r.status + ": " + (await r.text()).slice(0, 200), { status: r.ok ? 200 : 502 });
    };
    if (kind === "prueba") {
      const pr = (await get(`profiles?id=eq.${user_id}&select=notify_email`))[0];
      const e = pr?.notify_email || await emailOf(user_id); if (!e) return new Response("tu cuenta no tiene correo: añádelo en Ajustes → Cuenta", { status: 400 });
      return send([{ email: e, name: "" }], "Prueba de correo de T lo traigo", layout("¡El correo funciona!", "Si lees esto, los correos de compras y facturas ya se envían correctamente.", ""));
    }
    const o = (await get(`orders?id=eq.${Number(order_id)}&select=*,order_items(*)`))[0];
    if (!o) return new Response("sin pedido", { status: 404 });
    const people = [...new Set([o.customer_id, o.placed_by].filter(Boolean))];
    const profs = await get(`profiles?id=in.(${people.join(",")})&select=id,full_name,email_notifications,notify_email`);
    const to: { email: string; name: string }[] = [];
    for (const p of profs) { if (p.email_notifications === false) continue; const e = p.notify_email || await emailOf(p.id); if (e && !to.some((t) => t.email === e)) to.push({ email: e, name: p.full_name || "" }); }
    if (!to.length) return new Response("sin destinatarios");
    let subject: string, html: string;
    const fees = Number(o.service_fee) + Number(o.gestion_fee) + Number(o.extra_fee || 0) + Number(o.delivery_fee) + Number(o.express_fee);
    if (kind === "pedido") {
      subject = `Pedido nº ${o.id} recibido`;
      html = layout("¡Hemos recibido tu pedido!", `Pedido nº ${o.id}. Te avisaremos cuando un repartidor lo acepte. Los precios son orientativos: pagarás lo que marque el ticket del súper.`,
        itemsTable(o.order_items, false) + `<table role="presentation" width="100%" style="margin-top:10px">${line("Productos (estimado)", eur(o.subtotal))}${fees > 0 ? line("Servicio, gestión y envío", eur(fees)) : ""}${line("Total estimado", eur(o.total), true)}</table>
        <p style="font-size:13px;color:#5D6B67;margin-top:14px">Entrega en: ${esc(o.delivery_address)}</p>`);
    } else {
      const inv = (await get(`invoices?order_id=eq.${o.id}&select=*`))[0];
      subject = inv ? `Factura ${inv.number} · pedido nº ${o.id} entregado` : `Pedido nº ${o.id} entregado · justificante`;
      const invBlock = inv ? `<div style="background:#F3F7F4;border-radius:14px;padding:14px 16px;margin-top:16px"><p style="margin:0 0 6px;font-weight:bold;color:#1F4D3F">Factura ${esc(inv.number)}</p>
        <table role="presentation" width="100%">${line("Compra en el súper (suplido)", eur(inv.suplidos))}${line("Servicio — base imponible", eur(inv.base))}${line(`IVA ${inv.vat_rate} %`, eur(inv.vat))}${line("Total", eur(inv.total), true)}</table>
        <p style="font-size:12px;color:#5D6B67;margin:8px 0 0">Emisor: ${esc(inv.issuer?.name)} · NIF ${esc(inv.issuer?.nif)}. El PDF está en la app (Pedidos → Ver factura).</p></div>`
        : `<p style="font-size:13px;color:#5D6B67;margin-top:14px">El justificante en PDF está en la app (Pedidos → Justificante del pedido).</p>`;
      const diff = o.ticket_total != null ? Number(o.ticket_total) - Number(o.subtotal) : 0;
      html = layout("¡Tu compra ya está en casa!", `Pedido nº ${o.id} entregado el ${new Date(o.updated_at).toLocaleString("es-ES", { timeZone: "Europe/Madrid", day: "numeric", month: "long", hour: "2-digit", minute: "2-digit" })}.`,
        itemsTable(o.order_items, true) + `<table role="presentation" width="100%" style="margin-top:10px">${line("Ticket del súper", eur(o.ticket_total ?? o.subtotal))}${fees > 0 ? line("Servicio, gestión y envío", eur(fees)) : ""}${line("Total", eur(o.final_total ?? o.total), true)}</table>` +
        (Math.abs(diff) >= 0.01 ? `<p style="font-size:13px;margin-top:8px;color:${diff > 0 ? "#B23A48" : "#2E6B57"}">La compra salió ${eur(Math.abs(diff))} ${diff > 0 ? "más cara" : "más barata"} que lo estimado.</p>` : "") + invBlock);
    }
    return send(to, subject, html);
  } catch (e) { return new Response("error " + (e as Error).message, { status: 500 }); }
});
