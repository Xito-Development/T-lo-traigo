// Passkeys (WebAuthn): registrar una passkey en la cuenta y entrar sin contraseña.
// rpID = tlotraigo.unocerobits.com (con /.well-known/assetlinks.json para la app de Android).
import { generateRegistrationOptions, verifyRegistrationResponse, generateAuthenticationOptions, verifyAuthenticationResponse } from "npm:@simplewebauthn/server@13.1.1";

const SB = Deno.env.get("SUPABASE_URL")!;
const SR = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const H = { apikey: SR, Authorization: `Bearer ${SR}`, "Content-Type": "application/json" };
const RP_ID = "tlotraigo.unocerobits.com";
// Orígenes aceptados: la web y la app firmada (huella SHA-256 del certificado, en base64url)
const ORIGINS = ["https://tlotraigo.unocerobits.com", "android:apk-key-hash:wsJ7LY5AlAlwnBPapsLro61fi0f18xlywtzTO1QTvSo",
  ...(Deno.env.get("PASSKEY_EXTRA_ORIGINS") ?? "").split(",").filter(Boolean)];
const CORS = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type" };
const json = (b: unknown, s = 200) => new Response(JSON.stringify(b), { status: s, headers: { ...CORS, "Content-Type": "application/json" } });
const rest = (p: string, init: RequestInit = {}) => fetch(`${SB}/rest/v1/${p}`, { ...init, headers: { ...H, ...(init.headers || {}) } });
const b64u = (u: Uint8Array) => btoa(String.fromCharCode(...u)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const fromB64u = (s: string) => Uint8Array.from(atob(s.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((s.length + 3) % 4)), (c) => c.charCodeAt(0));

async function userFrom(req: Request) {
  const tok = (req.headers.get("authorization") || "").replace(/^Bearer\s+/i, "");
  if (!tok || tok === ANON) return null;
  const r = await fetch(`${SB}/auth/v1/user`, { headers: { apikey: ANON, Authorization: `Bearer ${tok}` } });
  return r.ok ? await r.json() : null;
}
async function saveChallenge(challenge: string, kind: string, userId: string | null) {
  const r = await rest("webauthn_challenges", { method: "POST", headers: { Prefer: "return=representation" }, body: JSON.stringify({ challenge, kind, user_id: userId }) });
  return (await r.json())[0].id as string;
}
async function takeChallenge(id: string, kind: string) {
  const r = await rest(`webauthn_challenges?id=eq.${id}&kind=eq.${kind}&select=*`); const c = (await r.json())[0];
  await rest(`webauthn_challenges?id=eq.${id}`, { method: "DELETE" });
  if (!c || Date.now() - new Date(c.created_at).getTime() > 5 * 60e3) throw new Error("El reto ha caducado, inténtalo de nuevo");
  return c;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  try {
    const body = await req.json(); const action = body.action;
    if (action === "register-options") {
      const u = await userFrom(req); if (!u) return json({ error: "Inicia sesión" }, 401);
      const mine = await (await rest(`passkeys?user_id=eq.${u.id}&select=credential_id,transports`)).json();
      const opts = await generateRegistrationOptions({ rpName: "T lo traigo", rpID: RP_ID, userName: u.email, userDisplayName: u.user_metadata?.full_name || u.email,
        userID: new TextEncoder().encode(u.id), attestationType: "none",
        excludeCredentials: mine.map((c: any) => ({ id: c.credential_id, transports: c.transports })),
        authenticatorSelection: { residentKey: "required", userVerification: "preferred" } });
      const challengeId = await saveChallenge(opts.challenge, "registro", u.id);
      return json({ challengeId, options: opts });
    }
    if (action === "register-verify") {
      const u = await userFrom(req); if (!u) return json({ error: "Inicia sesión" }, 401);
      const c = await takeChallenge(body.challengeId, "registro");
      if (c.user_id !== u.id) return json({ error: "Reto no válido" }, 400);
      const v = await verifyRegistrationResponse({ response: body.response, expectedChallenge: c.challenge, expectedOrigin: ORIGINS, expectedRPID: RP_ID, requireUserVerification: false });
      if (!v.verified || !v.registrationInfo) return json({ error: "No se pudo verificar la passkey" }, 400);
      const cred = v.registrationInfo.credential;
      await rest("passkeys", { method: "POST", body: JSON.stringify({ user_id: u.id, credential_id: cred.id, public_key: b64u(cred.publicKey), counter: cred.counter,
        transports: cred.transports ?? [], name: (body.name || "Passkey").slice(0, 60) }) });
      return json({ ok: true });
    }
    if (action === "login-options") {
      const opts = await generateAuthenticationOptions({ rpID: RP_ID, userVerification: "preferred", allowCredentials: [] });
      const challengeId = await saveChallenge(opts.challenge, "acceso", null);
      return json({ challengeId, options: opts });
    }
    if (action === "login-verify") {
      const c = await takeChallenge(body.challengeId, "acceso");
      const pk = (await (await rest(`passkeys?credential_id=eq.${encodeURIComponent(body.response.id)}&select=*`)).json())[0];
      if (!pk) return json({ error: "Esta passkey no está registrada en T lo traigo" }, 404);
      const v = await verifyAuthenticationResponse({ response: body.response, expectedChallenge: c.challenge, expectedOrigin: ORIGINS, expectedRPID: RP_ID,
        credential: { id: pk.credential_id, publicKey: fromB64u(pk.public_key), counter: Number(pk.counter), transports: pk.transports }, requireUserVerification: false });
      if (!v.verified) return json({ error: "Passkey no válida" }, 401);
      await rest(`passkeys?id=eq.${pk.id}`, { method: "PATCH", body: JSON.stringify({ counter: v.authenticationInfo.newCounter, last_used: new Date().toISOString() }) });
      // Sesión de Supabase para ese usuario: enlace de acceso generado y canjeado en el servidor
      const u = await (await fetch(`${SB}/auth/v1/admin/users/${pk.user_id}`, { headers: H })).json();
      const g = await (await fetch(`${SB}/auth/v1/admin/generate_link`, { method: "POST", headers: H, body: JSON.stringify({ type: "magiclink", email: u.email }) })).json();
      const tokenHash = g.hashed_token || g.properties?.hashed_token;
      const s = await fetch(`${SB}/auth/v1/verify`, { method: "POST", headers: { apikey: ANON, "Content-Type": "application/json" }, body: JSON.stringify({ type: "magiclink", token_hash: tokenHash }) });
      if (!s.ok) return json({ error: "No se pudo iniciar la sesión" }, 500);
      return json({ session: await s.json() });
    }
    return json({ error: "Acción no válida" }, 400);
  } catch (e) { return json({ error: (e as Error).message }, 400); }
});
