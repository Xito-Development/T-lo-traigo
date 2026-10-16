// Vigilante: cada 10 min revisa la plataforma y avisa por correo al superadministrador de lo nuevo (alta: al momento;
// recordatorio cada 24 h si sigue; «resuelto» cuando desaparece). Cada mañana envía un resumen completo.
const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const H = { apikey: SR, Authorization: `Bearer ${SR}`, "Content-Type": "application/json" };
const FROM = { name: "T lo traigo · Vigilante", email: "tlotraigo@unocerobits.com" };
const LOGO = "https://raw.githubusercontent.com/Xito-Development/T-lo-traigo/main/web/t-lo-traigo/email/logo.png";
const REPO = "Xito-Development/T-lo-traigo";
const get = async (p: string) => (await fetch(`${SB}/rest/v1/${p}`, { headers: H })).json();
const eur = (n: number) => (Number(n) || 0).toLocaleString("es-ES", { style: "currency", currency: "EUR" });
const esc = (s: unknown) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]!));
type Alert = { key: string; sev: "alta" | "media" | "baja"; title: string; body: string };

async function timed(url: string) {
  const t0 = Date.now();
  try { const r = await fetch(url, { signal: AbortSignal.timeout(10000) }); await r.body?.cancel(); return { ok: r.status < 400, status: r.status, ms: Date.now() - t0 }; }
  catch (e) { return { ok: false, status: 0, ms: Date.now() - t0, err: (e as Error).message }; }
}

async function collect(): Promise<{ alerts: Alert[]; d: any; extra: any }> {
  const d = await (await fetch(`${SB}/rest/v1/rpc/vigilante_data`, { method: "POST", headers: H, body: "{}" })).json();
  const A: Alert[] = []; const add = (key: string, sev: Alert["sev"], title: string, body: string) => A.push({ key, sev, title, body });
  // Servicios externos
  const web = await timed("https://tlotraigo.unocerobits.com/");
  if (!web.ok) add("web-caida", "alta", "La web no responde", `tlotraigo.unocerobits.com: ${web.status || web.err}. Revisa IONOS y el DNS.`);
  else if (web.ms > 4000) add("web-lenta", "media", "La web va lenta", `Responde en ${web.ms} ms.`);
  const auth = await timed(`${SB}/auth/v1/health?apikey=${Deno.env.get("SUPABASE_ANON_KEY") ?? ""}`);
  if (!auth.ok && auth.status !== 401) add("auth-caida", "alta", "El inicio de sesión falla", `Autenticación: ${auth.status || auth.err}.`);
  let run: any = null;
  try { run = (await (await fetch(`https://api.github.com/repos/${REPO}/actions/runs?per_page=5`, { headers: { "User-Agent": "tlotraigo-vigilante" } })).json()).workflow_runs?.find((r: any) => r.status === "completed"); } catch { /* sin GitHub */ }
  if (run && run.conclusion === "failure") add(`ci-${run.id}`, "alta", `Ha fallado la compilación «${run.name}»`, `${(run.head_commit?.message || "").split("\n")[0]} · ${run.html_url}`);
  // Plataforma
  for (const o of d.pendientes) add(`pend-${o.id}`, "alta", `Pedido nº ${o.id} sin repartidor`, `Lleva ${o.h} h esperando.`);
  for (const o of d.reclamados) add(`recl-${o.id}`, "alta", `Pedido nº ${o.id} reclamado`, `Plazo hasta ${new Date(o.limite).toLocaleString("es-ES", { timeZone: "Europe/Madrid" })}.`);
  for (const o of d.sin_cobrar) add(`cobro-${o.id}`, "media", `Pedido nº ${o.id} entregado sin cobrar`, `${eur(o.total)} pendiente desde hace más de un día.`);
  for (const c of d.reclamaciones) add(`hoja-${c.id}`, c.dias >= 7 ? "alta" : "media", `Hoja de reclamaciones ${c.numero || c.id}${c.dias >= 7 ? " sin responder (más de 7 días)" : ""}`, esc(c.asunto));
  for (const r of d.reembolsos) add(`reemb-${r.id}`, "media", `Reembolso pendiente · pedido nº ${r.pedido}`, r.importe ? eur(r.importe) : "Importe por decidir");
  for (const o of d.entidades) {
    if (o.verif === "pendiente") add(`verif-${o.id}`, "alta", `${o.nombre} espera verificación`, "Revisa su CIF/NIF y el documento y verifícala.");
    if (o.activa && o.sin_pago) add(`pago-${o.id}`, "alta", `${o.nombre} no tiene formas de pago`, "Sus usuarios no pueden completar pedidos.");
    if (o.hasta) { const days = Math.round((new Date(o.hasta).getTime() - Date.now()) / 864e5);
      if (days < 0 && o.activa) add(`venc-${o.id}`, "alta", `${o.nombre}: suscripción vencida`, `Venció hace ${-days} días; se desactiva sola a los 7.`);
      else if (days >= 0 && days <= 7) add(`renov-${o.id}`, "media", `${o.nombre}: renovación en ${days} días`, `Envía la factura de la cuota (${eur(o.cuota)}).`); }
  }
  for (const e of d.errores) add(`err-${e.source}-${e.name}-${e.app_version}`, e.n >= 5 ? "alta" : "media", `Error en ${e.source === "app" ? "la app" : "la web"}: ${e.name}`, `${e.n} veces en 24 h · versión ${e.app_version || "—"}.`);
  if (d.correo_fallos.length) add("correo-fallos", "alta", "Los correos de compras fallan", esc(d.correo_fallos[0].msg));
  return { alerts: A, d, extra: { web, run } };
}

function layout(title: string, intro: string, body: string) {
  return `<!doctype html><html lang="es"><body style="margin:0;background:#F3F7F4;font-family:Verdana,Arial,sans-serif;color:#1C2624">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:22px 12px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#fff;border-radius:20px;overflow:hidden">
<tr><td style="background:#1F4D3F;padding:18px 24px"><img src="${LOGO}" width="40" height="40" alt="" style="vertical-align:middle;border-radius:50%;border:2px solid #FFF8EC">
<span style="vertical-align:middle;margin-left:10px;font-size:20px;font-weight:bold;color:#FFF8EC">T lo traigo <span style="color:#F2B544">· Vigilante</span></span></td></tr>
<tr><td style="padding:24px 26px 6px"><h1 style="margin:0 0 8px;font-size:22px;color:#1F4D3F">${title}</h1><p style="margin:0 0 14px;font-size:15px;line-height:1.5;color:#4F5D59">${intro}</p></td></tr>
<tr><td style="padding:0 26px 24px">${body}</td></tr>
<tr><td style="background:#17382E;padding:14px;font-size:12px;color:#B9C7C2;text-align:center">Avisos del superadministrador · se desactivan en la app: Ajustes → Cuenta</td></tr>
</table></td></tr></table></body></html>`;
}
const color = { alta: "#C2413B", media: "#B07C00", baja: "#2E6B57" };
const items = (list: Alert[], label?: string) => list.length ? (label ? `<p style="margin:16px 0 6px;font-weight:bold;color:#1F4D3F">${label}</p>` : "") + list.map((a) =>
  `<div style="border-left:4px solid ${color[a.sev]};background:#F7FAF8;border-radius:8px;padding:10px 12px;margin:6px 0">
  <span style="font-size:11px;font-weight:bold;color:${color[a.sev]};text-transform:uppercase">${a.sev}</span>
  <div style="font-size:15px;font-weight:bold;margin:2px 0">${esc(a.title)}</div><div style="font-size:13px;color:#4F5D59">${a.body}</div></div>`).join("") : "";

Deno.serve(async (req) => {
  try {
    const secret = (await get("app_secrets?key=eq.correos&select=value"))[0]?.value;
    if (!secret || req.headers.get("x-tlt-secret") !== secret) return new Response("no autorizado", { status: 401 });
    const BREVO = Deno.env.get("BREVO_API_KEY") || (await get("app_secrets?key=eq.brevo_api_key&select=value"))[0]?.value || "";
    const { mode } = await req.json().catch(() => ({ mode: "check" }));
    const { alerts, d, extra } = await collect();
    const to = (d.destinatarios || []).map((x: any) => ({ email: x.correo, name: x.nombre || "" }));
    const prev: any[] = await get("admin_alerts?select=*&resolved_at=is.null");
    const prevMap = new Map(prev.map((p) => [p.key, p])); const now = new Date().toISOString();
    const nuevos: Alert[] = [], recordar: Alert[] = [];
    for (const a of alerts) {
      const p = prevMap.get(a.key);
      if (!p) nuevos.push(a);
      else if (a.sev === "alta" && (!p.last_sent || Date.now() - new Date(p.last_sent).getTime() > 24 * 3600e3)) recordar.push(a);
    }
    const keys = new Set(alerts.map((a) => a.key));
    const resueltos = prev.filter((p) => !keys.has(p.key));
    // Guardar estado
    const upserts = alerts.map((a) => ({ key: a.key, severity: a.sev, title: a.title, body: a.body, last_seen: now, resolved_at: null,
      ...(prevMap.get(a.key) ? {} : { first_seen: now }) }));
    if (upserts.length) await fetch(`${SB}/rest/v1/admin_alerts?on_conflict=key`, { method: "POST", headers: { ...H, Prefer: "resolution=merge-duplicates" }, body: JSON.stringify(upserts) });
    for (const r of resueltos) await fetch(`${SB}/rest/v1/admin_alerts?key=eq.${encodeURIComponent(r.key)}`, { method: "PATCH", headers: H, body: JSON.stringify({ resolved_at: now }) });

    let subject = "", html = "";
    const altasNuevas = nuevos.filter((a) => a.sev === "alta");
    if (mode === "resumen") {
      const order = { alta: 0, media: 1, baja: 2 }; const all = [...alerts].sort((a, b) => order[a.sev] - order[b.sev]);
      subject = all.some((a) => a.sev === "alta") ? `⚠️ Resumen diario: ${all.filter((a) => a.sev === "alta").length} asuntos urgentes` : "✅ Resumen diario de T lo traigo";
      html = layout("Resumen del día", `Pedidos hoy: <b>${d.pedidos_hoy}</b> · entregados ayer: <b>${d.entregados_ayer}</b> · web: <b>${extra.web.ok ? "funciona (" + extra.web.ms + " ms)" : "caída"}</b>` +
        (extra.run ? ` · última compilación: <b>${extra.run.conclusion === "success" ? "correcta" : "fallida"}</b>` : ""),
        all.length ? items(all) : `<p style="font-size:15px">Todo en orden: no hay nada que revisar. 🎉</p>`);
    } else if (altasNuevas.length || recordar.length || resueltos.some((r) => r.severity === "alta")) {
      subject = altasNuevas.length ? `⚠️ ${altasNuevas[0].title}${altasNuevas.length > 1 ? ` (+${altasNuevas.length - 1})` : ""}` : recordar.length ? `🔁 Sigue pendiente: ${recordar[0].title}` : "✅ Resuelto";
      html = layout(altasNuevas.length ? "Hay algo que revisar" : "Actualización de avisos", "Esto es lo que ha cambiado desde la última comprobación.",
        items(altasNuevas, "Nuevo") + items(recordar, "Sigue pendiente (recordatorio diario)") +
        items(resueltos.filter((r) => r.severity === "alta").map((r) => ({ key: r.key, sev: "baja" as const, title: "✅ " + r.title, body: "Ya no aparece." })), "Resuelto"));
    }
    if (!subject) return new Response(`sin novedades (${alerts.length} avisos activos)`);
    if (!BREVO) return new Response("hay avisos pero falta la clave de Brevo");
    if (!to.length) return new Response("hay avisos pero el superadministrador no tiene correo (Ajustes → Cuenta)");
    const r = await fetch("https://api.brevo.com/v3/smtp/email", { method: "POST", headers: { "api-key": BREVO, "Content-Type": "application/json", accept: "application/json" },
      body: JSON.stringify({ sender: FROM, to, subject, htmlContent: html, tags: ["vigilante", mode] }) });
    if (r.ok) {
      const sent = [...nuevos, ...recordar].filter((a) => mode === "resumen" || a.sev === "alta").map((a) => a.key);
      if (mode === "resumen") sent.push(...alerts.map((a) => a.key));
      if (sent.length) await fetch(`${SB}/rest/v1/admin_alerts?key=in.(${[...new Set(sent)].map((k) => `"${k}"`).join(",")})`, { method: "PATCH", headers: H, body: JSON.stringify({ last_sent: now }) });
    }
    return new Response(r.ok ? `enviado (${subject})` : "error de Brevo " + r.status + ": " + (await r.text()).slice(0, 200), { status: r.ok ? 200 : 502 });
  } catch (e) { return new Response("error " + (e as Error).message, { status: 500 }); }
});
