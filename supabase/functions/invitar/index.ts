// Invitación por correo: crea (si hace falta) la cuenta pendiente y envía un enlace para poner la contraseña y unirse a la entidad.
const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const H = { apikey: SR, Authorization: `Bearer ${SR}`, "Content-Type": "application/json" };
const FROM = { name: "T lo traigo", email: "tlotraigo@unocerobits.com" };
const WEB = "https://tlotraigo.unocerobits.com";
const LOGO = "https://raw.githubusercontent.com/Xito-Development/T-lo-traigo/main/web/t-lo-traigo/email/logo.png";
const get = async (p: string) => (await fetch(`${SB}/rest/v1/${p}`, { headers: H })).json();
const esc = (s: unknown) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]!));
const ROL: Record<string, string> = { gestor: "administrador/a del servicio", repartidor: "repartidor/a", cliente: "usuario/a" };

Deno.serve(async (req) => {
  try {
    const secret = (await get("app_secrets?key=eq.correos&select=value"))[0]?.value;
    if (!secret || req.headers.get("x-tlt-secret") !== secret) return new Response("no autorizado", { status: 401 });
    const BREVO = Deno.env.get("BREVO_API_KEY") || (await get("app_secrets?key=eq.brevo_api_key&select=value"))[0]?.value || "";
    if (!BREVO) return new Response("correo no configurado: falta la clave de Brevo");
    const { code } = await req.json();
    const inv = (await get(`org_invites?code=eq.${encodeURIComponent(code)}&select=*,organizations(name)`))[0];
    if (!inv?.invited_email) return new Response("invitación no encontrada", { status: 404 });
    const org = inv.organizations?.name || "T lo traigo";
    // Cuenta nueva: enlace con token para poner la contraseña en la web (sin depender de los correos de Supabase)
    let link = `${WEB}/unirse.html?c=${inv.code}`, nueva = false;
    const g = await fetch(`${SB}/auth/v1/admin/generate_link`, { method: "POST", headers: H,
      body: JSON.stringify({ type: "invite", email: inv.invited_email, data: { full_name: inv.invited_name || "", invite_code: inv.code } }) });
    if (g.ok) { const j = await g.json(); const t = j.hashed_token || j.properties?.hashed_token;
      if (t) { link += `&t=${encodeURIComponent(t)}&e=${encodeURIComponent(inv.invited_email)}`; nueva = true; } }
    const rol = ROL[inv.role] || "usuario/a";
    const hola = inv.invited_name ? `Hola, ${esc(inv.invited_name.split(" ")[0])}:` : "Hola:";
    const html = `<!doctype html><html lang="es"><body style="margin:0;background:#F3F7F4;font-family:Verdana,Arial,sans-serif;color:#1C2624">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:24px 12px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:580px;background:#fff;border-radius:20px;overflow:hidden">
<tr><td align="center" style="background:#2E6B57;padding:26px 24px 20px"><img src="${LOGO}" width="64" height="64" alt="T lo traigo" style="display:block;border:3px solid #FFF8EC;border-radius:50%;margin:0 auto 10px">
<p style="margin:0;font-family:'Trebuchet MS',Verdana,sans-serif;font-size:32px;font-weight:bold;color:#FFF8EC">T lo traigo</p>
<div style="width:140px;height:5px;margin:6px auto 0;background:#F2B544;border-radius:0 0 80px 80px"></div></td></tr>
<tr><td style="padding:28px 30px 8px"><h1 style="margin:0 0 10px;font-size:24px;color:#1F4D3F">${hola} te invitan a T lo traigo</h1>
<p style="margin:0 0 14px;font-size:16px;line-height:1.55"><b>${esc(org)}</b> te ha invitado a unirte como <b>${rol}</b> en T lo traigo, la app de compra a domicilio para las personas mayores de los pueblos.</p></td></tr>
<tr><td align="center" style="padding:6px 30px 18px"><a href="${link}" style="display:inline-block;background:#F2B544;color:#1B2428;font-weight:bold;font-size:17px;text-decoration:none;padding:15px 30px;border-radius:14px">${nueva ? "Crear mi cuenta" : "Aceptar la invitación"}</a></td></tr>
<tr><td style="padding:0 30px 10px"><ol style="margin:0;padding-left:20px;font-size:15px;line-height:1.7;color:#4F5D59">
${nueva ? "<li>Pulsa «Crear mi cuenta» y elige tu contraseña.</li><li>Descarga la app T lo traigo.</li><li>Entra con este correo y tu contraseña.</li>"
        : "<li>Abre la app T lo traigo con tu cuenta.</li><li>Ve a Ajustes → «Tengo un código» y escribe el código de abajo.</li>"}
</ol></td></tr>
<tr><td align="center" style="padding:6px 30px 22px"><div style="display:inline-block;background:#F3F7F4;border-radius:12px;padding:10px 18px;font-size:13px;color:#4F5D59">Código de invitación<br>
<span style="font-family:Consolas,monospace;font-size:24px;font-weight:bold;letter-spacing:4px;color:#1F4D3F">${inv.code}</span></div>
<p style="font-size:12px;color:#7A8A85;margin:10px 0 0">Caduca el ${new Date(inv.expires_at).toLocaleDateString("es-ES", { day: "numeric", month: "long" })}. Si no esperabas esta invitación, ignora este correo.</p></td></tr>
<tr><td align="center" style="background:#17382E;padding:18px;font-size:12.5px;line-height:1.7;color:#B9C7C2">¿Dudas? Responde a este correo · <a href="${WEB}" style="color:#fff">tlotraigo.unocerobits.com</a></td></tr>
</table></td></tr></table></body></html>`;
    const r = await fetch("https://api.brevo.com/v3/smtp/email", { method: "POST", headers: { "api-key": BREVO, "Content-Type": "application/json", accept: "application/json" },
      body: JSON.stringify({ sender: FROM, replyTo: { email: "tlotraigo@unocerobits.com", name: "T lo traigo" },
        to: [inv.invited_name ? { email: inv.invited_email, name: inv.invited_name } : { email: inv.invited_email }],
        subject: `${org} te invita a T lo traigo`, htmlContent: html,
        textContent: `${org} te ha invitado a unirte como ${rol} en T lo traigo. ${nueva ? "Crea tu cuenta" : "Acepta la invitación"}: ${link}  ·  Código: ${inv.code}` }) });
    return new Response(r.ok ? `invitación enviada a ${inv.invited_email}${nueva ? " (cuenta nueva)" : ""}` : "error de Brevo " + r.status + ": " + (await r.text()).slice(0, 200), { status: r.ok ? 200 : 502 });
  } catch (e) { return new Response("error " + (e as Error).message, { status: 500 }); }
});
