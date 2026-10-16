// T lo traigo Control — interfaz
'use strict';
const SB = 'https://qqqudthqgpojbipsevna.supabase.co';
const KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFxcXVkdGhxZ3BvamJpcHNldm5hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE0NDIxODUsImV4cCI6MjEwNzAxODE4NX0.GDqWTmFMgu_DX7oZ3YGcf1HHgT7-FIOJ7n8ctjRUc-w';
const REPO = 'Xito-Development/T-lo-traigo';
const WEB = 'tlotraigo.unocerobits.com';
const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmtDate = d => d ? new Date(d).toLocaleString('es-ES', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—';
const eur = n => (Number(n) || 0).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
let session = null, prefs = { interval: 5, notify: true, autostart: false, tray: true, dark: false }, state = {}, prevLevels = {}, page = 'resumen', timer = null;

function show(id) { $$('.screen').forEach(s => s.classList.remove('show')); $(id).classList.add('show'); }
function toast(t) { const d = document.createElement('div'); d.className = 'toast'; d.textContent = t; $('#toast').appendChild(d); setTimeout(() => d.remove(), 4200); }
$$('[data-w]').forEach(b => b.onclick = () => window.tlt.win(b.dataset.w));

// ---------------- Supabase ----------------
async function api(path, opts = {}) {
  const r = await fetch(SB + path, { ...opts, headers: { apikey: KEY, 'Content-Type': 'application/json', ...(session ? { Authorization: 'Bearer ' + session.access_token } : {}), ...(opts.headers || {}) } });
  if (r.status === 401 && session && !opts._retried) { await refresh(); return api(path, { ...opts, _retried: true }); }
  if (!r.ok) throw new Error((await r.text()).slice(0, 200) || ('HTTP ' + r.status));
  const t = await r.text(); return t ? JSON.parse(t) : null;
}
async function login(user, pass) {
  const email = user.includes('@') ? user.trim().toLowerCase() : user.trim().toLowerCase() + '@tlotraigo.app';
  const r = await fetch(SB + '/auth/v1/token?grant_type=password', { method: 'POST', headers: { apikey: KEY, 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password: pass }) });
  const j = await r.json(); if (!r.ok) throw new Error('Usuario o contraseña incorrectos');
  session = j; await checkRole(); return j;
}
async function refresh() {
  const r = await fetch(SB + '/auth/v1/token?grant_type=refresh_token', { method: 'POST', headers: { apikey: KEY, 'Content-Type': 'application/json' }, body: JSON.stringify({ refresh_token: session.refresh_token }) });
  if (!r.ok) { session = null; throw new Error('Sesión caducada'); }
  session = await r.json(); if (prefs.remember !== false) window.tlt.session.save(session);
}
async function checkRole() {
  const p = await api('/rest/v1/profiles?id=eq.' + session.user.id + '&select=role,full_name');
  if (!p[0] || p[0].role !== 'superadmin') { session = null; throw new Error('Esta cuenta no es de superadministrador'); }
  session.profile = p[0];
}

// ---------------- Arranque ----------------
(async () => {
  prefs = Object.assign(prefs, await window.tlt.prefs.get());
  document.body.classList.toggle('dark', !!prefs.dark);
  await new Promise(r => setTimeout(r, 2300));   // la animación de arranque se ve completa
  const saved = await window.tlt.session.load();
  if (saved && prefs.setupDone) {
    session = saved;
    try { await refresh(); await checkRole(); return startApp(); } catch { session = null; }
  }
  show('#wizard'); if (prefs.setupDone) goStep(1);
})();

// ---------------- Asistente ----------------
let step = 0;
function goStep(n) {
  step = n; $$('.wz-page').forEach(p => p.classList.toggle('on', +p.dataset.step === n));
  $$('.wz-steps li').forEach((l, i) => { l.classList.toggle('on', i === n); l.classList.toggle('done', i < n); });
}
$$('[data-next]').forEach(b => b.onclick = () => goStep(step + 1));
$('#loginForm').onsubmit = async e => {
  e.preventDefault(); $('#loginErr').textContent = ''; $('#loginBtn').disabled = true;
  try {
    await login($('#email').value, $('#pass').value);
    prefs.remember = $('#remember').checked; if (prefs.remember) await window.tlt.session.save(session);
    $('#pass').value = '';
    if (prefs.setupDone) startApp(); else goStep(2);
  } catch (err) { $('#loginErr').textContent = err.message; $('.wz-card').animate([{ transform: 'translateX(0)' }, { transform: 'translateX(-10px)' }, { transform: 'translateX(10px)' }, { transform: 'translateX(-6px)' }, { transform: 'translateX(0)' }], { duration: 400 }); }
  $('#loginBtn').disabled = false;
};
$$('#intervalSeg button').forEach(b => b.onclick = () => { $$('#intervalSeg button').forEach(x => x.classList.remove('on')); b.classList.add('on'); prefs.interval = +b.dataset.v; });
$('#prefDark').onchange = e => { prefs.dark = e.target.checked; document.body.classList.toggle('dark', prefs.dark); };
$('#finishWizard').onclick = async () => {
  Object.assign(prefs, { notify: $('#prefNotify').checked, autostart: $('#prefAuto').checked, tray: $('#prefTray').checked, setupDone: true });
  await window.tlt.prefs.set(prefs); startApp();
};
$('[data-step="2"] [data-next]').onclick = () => goStep(3);

// ---------------- Panel ----------------
function startApp() {
  show('#app'); $('#who').textContent = (session.profile?.full_name || 'Superadministrador') + ' · superadmin';
  $$('.nav').forEach(n => n.onclick = () => { page = n.dataset.page; $$('.nav').forEach(x => x.classList.toggle('on', x === n)); render(); });
  render(); poll();
}
function schedule() { clearTimeout(timer); nextAt = Date.now() + prefs.interval * 60000; timer = setTimeout(poll, prefs.interval * 60000); }

async function poll() {
  $('.spin')?.classList.add('on');
  try { await collect(); } catch (e) { console.error(e); }
  evaluate(); saveHistory(); render(); notifyChanges(); schedule(); checkDesktopUpdate();
}

// Recoge el estado de todos los servicios y los datos de la plataforma
async function collect() {
  const since = new Date(Date.now() - 30 * 864e5).toISOString();
  const tasks = {
    auth: window.tlt.http(SB + '/auth/v1/health', { headers: { apikey: KEY } }),
    rest: window.tlt.http(SB + '/rest/v1/', { headers: { apikey: KEY } }),
    fnPagos: window.tlt.http(SB + '/functions/v1/pagos', { method: 'OPTIONS' }),
    fnWebhook: window.tlt.http(SB + '/functions/v1/stripe-webhook', { method: 'POST' }),
    web: window.tlt.http('https://' + WEB + '/'),
    ssl: window.tlt.ssl(WEB),
    orders: api('/rest/v1/orders?select=id,status,created_at,updated_at,org_id,claimed_at,claim_deadline,driver_id,payment_status,payment_method,ticket_total,final_total,total&created_at=gte.' + since + '&order=created_at.desc&limit=3000'),
    orgs: api('/rest/v1/organizations?select=id,name,kind,active,plan,price_month,subscription_until,accepting_orders,allow_cash,allow_bizum_driver,allow_bizum_org,bizum_org_phone,verification_status,tax_id&order=name'),
    complaints: api('/rest/v1/complaints?status=eq.recibida&select=id,number,subject,created_at,order_id&order=created_at'),
    refunds: api('/rest/v1/refund_requests?status=eq.pendiente&select=id,order_id,amount,created_at,reason&order=created_at'),
    drivers: api('/rest/v1/driver_settings?select=driver_id,approved,available,town_ids,bizum_phone'),
    towns: api('/rest/v1/towns?select=id,name,org_id,active'),
    analytics: api('/rest/v1/rpc/analytics_summary', { method: 'POST', body: JSON.stringify({ p_days: 7 }) }),
    fees: api('/rest/v1/fee_config?id=eq.1&select=card_enabled,demo_payments'),
    active: api('/rest/v1/orders?status=not.in.(entregado,cancelado)&select=id,status,created_at,updated_at,org_id,town_id,driver_id,claimed_at,claim_deadline,payment_method,payment_status,total,final_total,delivery_address,scheduled_date,order_items(name,qty,detail,substitute,unavailable)&order=created_at'),
    mail: api('/rest/v1/rpc/mail_status', { method: 'POST', body: '{}' }),
  };
  const now = Date.now();
  if (!state.ghAt || now - state.ghAt > 10 * 60000) {   // GitHub: como mucho cada 10 minutos (límite de su API)
    tasks.release = fetch('https://api.github.com/repos/' + REPO + '/releases/latest').then(r => r.ok ? r.json() : null).catch(() => null);
    tasks.runs = fetch('https://api.github.com/repos/' + REPO + '/actions/runs?per_page=5').then(r => r.ok ? r.json() : null).catch(() => null);
    state.ghAt = now;
  }
  const keys = Object.keys(tasks), vals = await Promise.allSettled(Object.values(tasks));
  keys.forEach((k, i) => { state[k] = vals[i].status === 'fulfilled' ? vals[i].value : { error: vals[i].reason?.message || 'Error' }; });
  state.at = new Date();
}

// Estado de cada servicio y sugerencias
function evaluate() {
  const s = state, svcs = [];
  const httpSvc = (id, name, desc, r, okFn) => {
    if (!r) return svcs.push({ id, name, desc, level: 'unk', msg: 'Sin datos' });
    if (!r.ok) return svcs.push({ id, name, desc, level: 'bad', msg: r.error || 'No responde' });
    const good = okFn ? okFn(r.status) : r.status < 400;
    svcs.push({ id, name, desc, level: good ? (r.ms > 2500 ? 'warn' : 'ok') : 'bad', msg: (good ? 'Funciona' : 'Error ' + r.status) + ' · ' + r.ms + ' ms' });
  };
  httpSvc('rest', 'Base de datos', 'API de datos de Supabase (UE)', s.rest);
  httpSvc('auth', 'Inicio de sesión', 'Autenticación y códigos de correo', s.auth);
  httpSvc('fnPagos', 'Función «pagos»', 'Pagos, propinas y cobros opcionales', s.fnPagos, st => st < 500);
  httpSvc('fnWebhook', 'Función «stripe-webhook»', 'Avisos de la pasarela de pago', s.fnWebhook, st => st < 500);
  httpSvc('web', 'Web', WEB, s.web);
  if (s.ssl?.ok) { const days = Math.round((new Date(s.ssl.validTo) - Date.now()) / 864e5);
    svcs.push({ id: 'ssl', name: 'Certificado SSL', desc: 'Candado de la web · ' + (s.ssl.issuer || ''), level: days < 7 ? 'bad' : days < 21 ? 'warn' : 'ok', msg: 'Caduca en ' + days + ' días (' + new Date(s.ssl.validTo).toLocaleDateString('es-ES') + ')', days });
  } else svcs.push({ id: 'ssl', name: 'Certificado SSL', desc: 'Candado de la web', level: 'bad', msg: s.ssl?.error || 'No se pudo comprobar' });
  const run = s.runs?.workflow_runs?.[0];
  if (run) svcs.push({ id: 'ci', name: 'Compilación (GitHub)', desc: run.name + ' · ' + (run.head_commit?.message || '').split('\n')[0].slice(0, 60),
    level: run.status !== 'completed' ? 'warn' : run.conclusion === 'success' ? 'ok' : 'bad', msg: run.status !== 'completed' ? 'Compilando…' : (run.conclusion === 'success' ? 'Correcta' : 'Ha fallado') + ' · ' + fmtDate(run.updated_at), url: run.html_url });
  if (s.release?.tag_name) svcs.push({ id: 'rel', name: 'Última versión de la app', desc: 'Publicada en GitHub', level: 'ok', msg: s.release.tag_name + ' · ' + fmtDate(s.release.published_at), url: s.release.html_url });
  const dataErr = ['orders', 'orgs', 'analytics'].find(k => s[k]?.error);
  svcs.push({ id: 'rls', name: 'Permisos de superadministrador', desc: 'Lectura de pedidos, entidades y estadísticas', level: dataErr ? 'bad' : 'ok', msg: dataErr ? s[dataErr].error : 'Correctos' });
  state.svcs = svcs;

  // ---- Sugerencias ----
  const sug = [], orders = Array.isArray(s.orders) ? s.orders : [], now = Date.now(), H = 3600e3;
  const add = (sev, title, text) => sug.push({ sev, title, text });
  svcs.filter(v => v.level === 'bad').forEach(v => add('alta', v.name + ': ' + v.msg, 'Revisa el servicio. ' + (v.id === 'web' ? 'Comprueba el hosting de IONOS y el DNS del subdominio.' : v.id === 'ci' ? 'Abre GitHub Actions para ver el error de compilación.' : 'Mira el estado de Supabase (status.supabase.com) y los registros del proyecto.')));
  const ssl = svcs.find(v => v.id === 'ssl'); if (ssl?.days != null && ssl.days < 21) add(ssl.days < 7 ? 'alta' : 'media', 'Renueva el certificado SSL', 'Caduca en ' + ssl.days + ' días. Activa la renovación automática en IONOS.');
  orders.filter(o => o.status === 'pendiente' && now - new Date(o.created_at) > 2 * H).forEach(o => add('alta', 'Pedido nº ' + o.id + ' sin repartidor desde hace ' + Math.round((now - new Date(o.created_at)) / H) + ' h', 'Avisa a la entidad o a sus repartidores para que lo acepten.'));
  orders.filter(o => o.claimed_at && !['entregado', 'cancelado'].includes(o.status)).forEach(o => add('alta', 'Pedido nº ' + o.id + ' reclamado', 'Plazo hasta ' + fmtDate(o.claim_deadline) + '. Contacta con la entidad.'));
  orders.filter(o => o.status === 'entregado' && o.payment_status !== 'pagado' && now - new Date(o.updated_at) > 24 * H).forEach(o => add('media', 'Pedido nº ' + o.id + ' entregado sin cobrar', 'Lleva más de un día pendiente de cobro (' + eur(o.final_total || o.total) + ').'));
  (Array.isArray(s.complaints) ? s.complaints : []).filter(c => now - new Date(c.created_at) > 7 * 24 * H).forEach(c => add('alta', 'Reclamación ' + (c.number || c.id) + ' sin responder', 'Han pasado más de 7 días; el plazo legal es de 10 días hábiles.'));
  (Array.isArray(s.refunds) ? s.refunds : []).forEach(r => add('media', 'Reembolso pendiente del pedido nº ' + r.order_id, (r.amount ? eur(r.amount) + ' · ' : '') + esc(r.reason || '').slice(0, 90)));
  (Array.isArray(s.orgs) ? s.orgs : []).forEach(o => {
    if (o.subscription_until) { const d = Math.round((new Date(o.subscription_until) - now) / 864e5);
      if (d < 0 && o.active) add('alta', o.name + ': suscripción vencida', 'Venció hace ' + (-d) + ' días; se desactivará sola a los 7 días.');
      else if (d >= 0 && d <= 7) add('media', o.name + ': renovación en ' + d + ' días', 'Envía la factura de la cuota (' + eur(o.price_month) + ').'); }
    if (o.verification_status === 'pendiente') add('alta', o.name + ' espera verificación', 'Revisa su CIF/NIF y el documento en la app (Admin → Entidades) y verifícala.');
    if (o.verification_status === 'sin_datos' && o.active) add('media', o.name + ' no ha enviado sus datos fiscales', 'Sin verificar no puede recibir pedidos.');
    if (!o.active) add('baja', o.name + ' está desactivada', 'Sus usuarios no pueden pedir.');
    if (o.active && !o.accepting_orders) add('baja', o.name + ' tiene los pedidos en pausa', 'Comprueba si es intencionado.');
    if (o.active && !o.allow_cash && !o.allow_bizum_driver && !(o.allow_bizum_org && o.bizum_org_phone)) add('alta', o.name + ' no tiene formas de pago', 'Sus usuarios no pueden completar pedidos.');
    const tIds = (s.towns || []).filter(t => t.org_id === o.id && t.active).map(t => t.id);
    if (o.active && tIds.length && !(s.drivers || []).some(d => d.approved && (d.town_ids || []).some(id => tIds.includes(id)))) add('media', o.name + ' no tiene repartidores autorizados', 'Envía un código de repartidor desde su panel.');
  });
  const errs = s.analytics?.errors || []; if (errs.length >= 5) add('media', errs.length + ' errores registrados esta semana', 'Revisa la pestaña Errores para ver los más repetidos.');
  if (s.fees?.[0]?.demo_payments && s.fees?.[0]?.card_enabled) add('alta', 'Pagos simulados activos con tarjeta encendida', 'Desactiva el modo demostración en Administración → Tarifas.');
  const lastAct = orders[0]?.created_at; if (!lastAct || now - new Date(lastAct) > 6 * 24 * H) add('baja', 'Sin pedidos en los últimos 6 días', 'En el plan gratuito, Supabase pausa el proyecto tras 7 días sin uso. Pasa a Pro con el primer cliente.');
  if (s.mail?.db_key === false && (s.mail?.last?.[0]?.msg || '').includes('falta')) add('alta', 'Los correos de compras no se envían', 'Falta la clave de Brevo: pégala en la página Correo.');
  state.sug = sug;
  const worst = svcs.some(v => v.level === 'bad') ? 'bad' : svcs.some(v => v.level === 'warn') || sug.some(x => x.sev === 'alta') ? 'warn' : 'ok';
  state.level = worst;
  const tb = $('#tbStatus'); tb.className = 'tb-status ' + worst;
  const text = worst === 'ok' ? 'Todo funciona' : worst === 'warn' ? 'Hay avisos' : 'Hay fallos';
  tb.querySelector('span').textContent = text + ' · ' + state.at.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  window.tlt.trayStatus(worst, text);
  const setBadge = (id, n) => { const b = $(id); b.textContent = n; b.classList.toggle('show', n > 0); };
  setBadge('#badgeErr', errs.length); setBadge('#badgeSug', sug.filter(x => x.sev !== 'baja').length);
  setBadge('#badgePen', (s.complaints?.length || 0) + (s.refunds?.length || 0));
}

function notifyChanges() {
  if (!prefs.notify || !state.svcs) return;
  state.svcs.forEach(v => { const prev = prevLevels[v.id];
    if (prev && prev !== v.level) {
      if (v.level === 'bad') window.tlt.notify('⚠️ ' + v.name + ' falla', v.msg);
      else if (v.level === 'ok' && prev === 'bad') window.tlt.notify('✅ ' + v.name + ' vuelve a funcionar', v.msg);
    }
    prevLevels[v.id] = v.level; });
  const claims = (state.orders || []).filter?.(o => o.claimed_at && !['entregado', 'cancelado'].includes(o.status)).length || 0;
  if (state.prevClaims != null && claims > state.prevClaims) window.tlt.notify('Pedido reclamado', 'Hay ' + claims + ' pedido(s) reclamado(s) sin entregar.');
  state.prevClaims = claims;
}

// ---------------- Páginas ----------------
const stag = i => 'style="animation-delay:' + (i * 60) + 'ms"';
function header(t, sub) { return '<div class="page-h"><div><h1>' + t + '</h1><p>' + sub + '</p></div><div class="refresh"><span class="spin"></span><span>' +
  (state.at ? 'Actualizado ' + state.at.toLocaleTimeString('es-ES') : 'Cargando…') + '</span><button class="btn ghost small" id="refreshBtn">Actualizar</button></div></div>'; }
function svcCard(v, i) { return '<div class="card svc" ' + stag(i) + (v.url ? ' data-url="' + esc(v.url) + '"' : '') + '><span class="dot ' + v.level + '"></span><div><h3>' + esc(v.name) + '</h3><div class="s">' + esc(v.desc) + '</div><div class="m">' + esc(v.msg) + '</div></div></div>'; }
function counter(el) { const to = +el.dataset.to; const t0 = performance.now(); const f = t => { const k = Math.min((t - t0) / 900, 1), e = 1 - Math.pow(1 - k, 3); el.textContent = Math.round(to * e).toLocaleString('es-ES'); if (k < 1) requestAnimationFrame(f); }; requestAnimationFrame(f); }

function renderBase() {
  const c = $('#content'), s = state;
  if (!s.svcs) { c.innerHTML = header('Resumen', 'Comprobando todos los servicios…') + '<div class="grid">' + '<div class="skel"></div>'.repeat(8) + '</div>'; return; }
  const orders = Array.isArray(s.orders) ? s.orders : [], today = new Date().toDateString();
  let h = '';
  if (page === 'resumen') {
    const okN = s.svcs.filter(v => v.level === 'ok').length;
    const title = s.level === 'ok' ? 'Todo funciona correctamente' : s.level === 'warn' ? 'Hay avisos que revisar' : 'Hay servicios con fallos';
    const kpis = [['Pedidos hoy', orders.filter(o => new Date(o.created_at).toDateString() === today).length], ['En curso', orders.filter(o => !['entregado', 'cancelado'].includes(o.status)).length],
      ['Entregados (30 días)', orders.filter(o => o.status === 'entregado').length], ['Entidades activas', (s.orgs || []).filter?.(o => o.active).length || 0],
      ['Repartidores disponibles', (s.drivers || []).filter?.(d => d.approved && d.available).length || 0], ['Errores (7 días)', s.analytics?.errors?.length || 0]];
    h = header('Resumen', 'Estado de T lo traigo de un vistazo') +
      '<div class="hero ' + s.level + '"><div class="orb"><i></i></div><div><h2>' + title + '</h2><p>' + okN + ' de ' + s.svcs.length + ' servicios en verde · ' + s.sug.length + ' sugerencias</p></div></div>' +
      '<div class="grid">' + kpis.map(([l, n], i) => '<div class="card kpi" ' + stag(i) + '><div class="n" data-to="' + n + '">0</div><div class="l">' + l + '</div></div>').join('') + '</div>' +
      '<h2 class="sec">Pedidos de los últimos 14 días</h2>' + chart(orders) +
      '<h2 class="sec">Servicios</h2><div class="grid">' + s.svcs.map(svcCard).join('') + '</div>';
  } else if (page === 'servicios') {
    h = header('Servicios', 'Comprobación cada ' + prefs.interval + ' min') + '<div class="grid">' + s.svcs.map(svcCard).join('') + '</div>';
  } else if (page === 'actividad') {
    const byStatus = {}; orders.forEach(o => byStatus[o.status] = (byStatus[o.status] || 0) + 1);
    const daily = s.analytics?.daily || [];
    h = header('Actividad', 'Últimos 30 días') + chart(orders) +
      '<h2 class="sec">Pedidos por estado</h2><div class="grid">' + ['pendiente', 'aceptado', 'comprando', 'en_camino', 'entregado', 'cancelado'].map((k, i) =>
        '<div class="card kpi" ' + stag(i) + '><div class="n" data-to="' + (byStatus[k] || 0) + '">0</div><div class="l">' + ({ pendiente: 'Pendientes', aceptado: 'Aceptados', comprando: 'En el súper', en_camino: 'En reparto', entregado: 'Entregados', cancelado: 'Cancelados' })[k] + '</div></div>').join('') + '</div>' +
      '<h2 class="sec">Visitas (con consentimiento, 7 días)</h2><div class="grid"><div class="card kpi"><div class="n" data-to="' + daily.reduce((a, d) => a + d.web, 0) + '">0</div><div class="l">Visitas a la web</div></div>' +
      '<div class="card kpi" ' + stag(1) + '><div class="n" data-to="' + daily.reduce((a, d) => a + d.app, 0) + '">0</div><div class="l">Pantallas vistas en la app</div></div>' +
      '<div class="card kpi" ' + stag(2) + '><div class="n" data-to="' + ((s.analytics?.visitors?.web || 0) + (s.analytics?.visitors?.app || 0)) + '">0</div><div class="l">Visitantes únicos</div></div></div>';
  } else if (page === 'errores') {
    const errs = s.analytics?.errors || [];
    h = header('Errores', 'Registrados por la app y la web en los últimos 7 días') + (errs.length ? '<div class="list">' + errs.map((e, i) =>
      '<div class="item" ' + stag(i) + '><span class="sev alta">' + esc(e.source) + '</span><div><h4>' + esc(e.name) + '</h4><p>' + fmtDate(e.ts) + (e.version ? ' · ' + esc(e.version) : '') + '</p>' +
      (e.detail ? '<pre>' + esc(JSON.stringify(e.detail, null, 1)).slice(0, 1500) + '</pre>' : '') + '</div></div>').join('') + '</div>' : empty('Sin errores', 'No se ha registrado ningún error esta semana.'));
  } else if (page === 'sugerencias') {
    const order = { alta: 0, media: 1, baja: 2 }, list = [...s.sug].sort((a, b) => order[a.sev] - order[b.sev]);
    h = header('Sugerencias', 'Lo que conviene revisar ahora') + (list.length ? '<div class="list">' + list.map((x, i) =>
      '<div class="item" ' + stag(i) + '><span class="sev ' + x.sev + '">' + x.sev + '</span><div><h4>' + esc(x.title) + '</h4><p>' + esc(x.text) + '</p></div></div>').join('') + '</div>' : empty('Nada que revisar', 'Todo está en orden.'));
  } else if (page === 'entidades') {
    const orgs = Array.isArray(s.orgs) ? s.orgs : [];
    h = header('Entidades', orgs.length + ' entidades') + '<table><thead><tr><th>Entidad</th><th>Verificación</th><th>Plan</th><th>Cuota</th><th>Pagada hasta</th><th>Estado</th><th>Pedidos (30 d)</th></tr></thead><tbody>' +
      orgs.map((o, i) => { const n = orders.filter(x => x.org_id === o.id).length; const st = !o.active ? '<span class="pill bad">Desactivada</span>' : !o.accepting_orders ? '<span class="pill warn">En pausa</span>' : '<span class="pill ok">Activa</span>';
        const ver = o.verification_status === 'verificada' ? '<span class="pill ok">Verificada</span>' : o.verification_status === 'pendiente' ? '<span class="pill warn">En revisión</span>' : o.verification_status === 'rechazada' ? '<span class="pill bad">Rechazada</span>' : '<span class="pill">Sin datos</span>';
        return '<tr ' + stag(i) + '><td><b>' + esc(o.name) + '</b><br><small>' + esc(o.kind) + (o.tax_id ? ' · ' + esc(o.tax_id) : '') + '</small></td><td>' + ver + '</td><td>' + esc(o.plan) + '</td><td>' + eur(o.price_month) + '</td><td>' + (o.subscription_until ? new Date(o.subscription_until).toLocaleDateString('es-ES') : '—') + '</td><td>' + st + '</td><td>' + n + '</td></tr>'; }).join('') + '</tbody></table>';
  } else if (page === 'pendientes') {
    const cs = Array.isArray(s.complaints) ? s.complaints : [], rs = Array.isArray(s.refunds) ? s.refunds : [];
    h = header('Pendientes', 'Reclamaciones y reembolsos por resolver') + '<h2 class="sec">Hojas de reclamaciones (' + cs.length + ')</h2>' +
      (cs.length ? '<div class="list">' + cs.map((c, i) => '<div class="item" ' + stag(i) + '><span class="sev media">' + esc(c.number || c.id) + '</span><div><h4>' + esc(c.subject) + '</h4><p>' + fmtDate(c.created_at) + (c.order_id ? ' · pedido nº ' + c.order_id : '') + '</p></div></div>').join('') + '</div>' : empty('Sin reclamaciones', 'Todas respondidas.')) +
      '<h2 class="sec">Reembolsos (' + rs.length + ')</h2>' + (rs.length ? '<div class="list">' + rs.map((r, i) => '<div class="item" ' + stag(i) + '><span class="sev media">' + eur(r.amount) + '</span><div><h4>Pedido nº ' + r.order_id + '</h4><p>' + esc(r.reason) + ' · ' + fmtDate(r.created_at) + '</p></div></div>').join('') + '</div>' : empty('Sin reembolsos', 'No hay solicitudes pendientes.'));
  } else if (page === 'ajustes') {
    h = header('Ajustes', 'Preferencias del panel') + '<div class="card" style="max-width:560px">' +
      '<div class="pref"><span>Comprobar cada</span><div class="seg" id="setInterval">' + [1, 5, 15].map(v => '<button data-v="' + v + '" class="' + (prefs.interval === v ? 'on' : '') + '">' + v + ' min</button>').join('') + '</div></div>' +
      sw('setNotify', 'Avisos cuando algo falle', prefs.notify) + sw('setAuto', 'Iniciar con Windows (en la bandeja)', prefs.autostart) + sw('setTray', 'Al cerrar, seguir vigilando en la bandeja', prefs.tray !== false) + sw('setDark', 'Modo oscuro', prefs.dark) +
      '<button class="btn ghost" id="logout" style="margin-top:8px">Cerrar sesión</button></div>' +
      '<p style="margin-top:16px;color:var(--muted);font-size:13px">T lo traigo Control 1.0 · UNOCEROBITS · Xito Development · info@unocerobits.com</p>';
  }
  c.innerHTML = '<div class="page">' + h + '</div>';
  $$('.kpi .n').forEach(counter);
  $$('[data-url]').forEach(e => { e.style.cursor = 'pointer'; e.onclick = () => window.tlt.open(e.dataset.url); });
  const rb = $('#refreshBtn'); if (rb) rb.onclick = poll;
  if (page === 'ajustes') bindSettings();
}
function sw(id, label, on) { return '<label class="switch"><input type="checkbox" id="' + id + '"' + (on ? ' checked' : '') + '><i></i> ' + label + '</label>'; }
function empty(t, d) { return '<div class="empty"><b>' + t + '</b>' + d + '</div>'; }
function bindSettings() {
  const save = () => window.tlt.prefs.set(prefs);
  $$('#setInterval button').forEach(b => b.onclick = () => { prefs.interval = +b.dataset.v; save(); schedule(); render(); toast('Comprobación cada ' + prefs.interval + ' min'); });
  $('#setNotify').onchange = e => { prefs.notify = e.target.checked; save(); };
  $('#setAuto').onchange = e => { prefs.autostart = e.target.checked; save(); };
  $('#setTray').onchange = e => { prefs.tray = e.target.checked; save(); };
  $('#setDark').onchange = e => { prefs.dark = e.target.checked; document.body.classList.toggle('dark', prefs.dark); save(); };
  $('#logout').onclick = async () => { await window.tlt.session.clear(); session = null; clearTimeout(timer); show('#wizard'); goStep(1); };
}
// Gráfica de barras animada de pedidos por día
function chart(orders) {
  const days = [...Array(14)].map((_, i) => { const d = new Date(); d.setHours(0, 0, 0, 0); d.setDate(d.getDate() - 13 + i); return d; });
  const counts = days.map(d => orders.filter(o => { const x = new Date(o.created_at); return x >= d && x < new Date(d.getTime() + 864e5); }).length);
  const max = Math.max(1, ...counts), W = 700, H = 200, bw = W / 14;
  return '<div class="chart"><svg viewBox="0 0 ' + W + ' ' + (H + 30) + '">' + counts.map((n, i) => { const h = n / max * (H - 20);
    return '<rect class="bar" style="animation-delay:' + (i * 40) + 'ms" x="' + (i * bw + 6) + '" y="' + (H - h) + '" width="' + (bw - 12) + '" height="' + Math.max(h, 2) + '" rx="6" fill="' + (i === 13 ? '#F2B544' : '#2E6B57') + '"/>' +
      (n ? '<text x="' + (i * bw + bw / 2) + '" y="' + (H - h - 6) + '" text-anchor="middle" font-size="12" fill="currentColor">' + n + '</text>' : '') +
      '<text x="' + (i * bw + bw / 2) + '" y="' + (H + 20) + '" text-anchor="middle" font-size="11" fill="#7A8A85">' + days[i].getDate() + '/' + (days[i].getMonth() + 1) + '</text>'; }).join('') + '</svg></div>';
}


// ======================= v1.1: más funciones =======================
let nextAt = 0, users = null, orderFilter = 'todos', orderQuery = '';
const statusName = { pendiente: 'Pendiente', aceptado: 'Aceptado', comprando: 'En el súper', en_camino: 'En reparto', entregado: 'Entregado', cancelado: 'Cancelado' };
const ago = d => { const m = Math.round((Date.now() - new Date(d)) / 60000); return m < 60 ? m + ' min' : m < 1440 ? Math.round(m / 60) + ' h' : Math.round(m / 1440) + ' d'; };
async function rpc(fn, args = {}) { return api('/rest/v1/rpc/' + fn, { method: 'POST', body: JSON.stringify(args) }); }

// --- Anillo de cuenta atrás hasta la próxima comprobación
setInterval(() => { const f = $('#ringFill'); if (!f || !nextAt) return; const total = prefs.interval * 60000, left = Math.max(0, nextAt - Date.now());
  f.style.strokeDashoffset = String(50.3 * (1 - left / total)); }, 1000);

// --- Historial de cada servicio (24 h) para gráficas y % de disponibilidad
function saveHistory() {
  const h = JSON.parse(localStorage.getItem('hist') || '{}'), now = Date.now();
  (state.svcs || []).forEach(v => { const m = /(\d+) ms/.exec(v.msg); (h[v.id] = h[v.id] || []).push([now, v.level === 'ok' || v.level === 'warn' ? 1 : 0, m ? +m[1] : null]);
    h[v.id] = h[v.id].filter(x => now - x[0] < 864e5).slice(-300); });
  localStorage.setItem('hist', JSON.stringify(h)); state.hist = h;
}
function spark(id) {
  const pts = (state.hist?.[id] || []).filter(x => x[2] != null); if (pts.length < 2) return '';
  const max = Math.max(...pts.map(x => x[2])), W = 200, H = 34, step = W / (pts.length - 1);
  const d = pts.map((x, i) => (i ? 'L' : 'M') + (i * step).toFixed(1) + ' ' + (H - 2 - (x[2] / max) * (H - 6)).toFixed(1)).join(' ');
  return '<svg class="spark" viewBox="0 0 ' + W + ' ' + H + '" preserveAspectRatio="none"><path class="area" d="' + d + ' L' + W + ' ' + H + ' L0 ' + H + 'Z"/><path d="' + d + '"/></svg>';
}
function uptime(id) { const h = state.hist?.[id] || []; if (!h.length) return ''; const up = h.filter(x => x[1]).length / h.length * 100;
  return '<div class="upt">Disponibilidad 24 h: <b>' + up.toFixed(up === 100 ? 0 : 1) + ' %</b> · ' + h.length + ' comprobaciones</div>'; }

// --- Modales
function modal(title, body, actions) {
  $('#mTitle').textContent = title; $('#mBody').innerHTML = body; const a = $('#mActions'); a.innerHTML = '';
  actions.concat([{ t: actions.length ? 'Cancelar' : 'Cerrar', cls: 'ghost' }]).forEach(x => { const b = document.createElement('button'); b.className = 'btn ' + (x.cls || 'primary'); b.textContent = x.t;
    b.onclick = async () => { if (x.fn) { b.disabled = true; try { await x.fn(); } catch (e) { toast('Error: ' + e.message); b.disabled = false; return; } } closeModal(); }; a.appendChild(b); });
  $('#modal').classList.add('open');
}
function closeModal() { $('#modal').classList.remove('open'); }
$('#modal').onclick = e => { if (e.target.id === 'modal') closeModal(); };

// --- Exportar a CSV (Excel)
function csv(name, rows) {
  const txt = '\uFEFF' + rows.map(r => r.map(v => '"' + String(v ?? '').replace(/"/g, '""') + '"').join(';')).join('\n');
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([txt], { type: 'text/csv' })); a.download = name; a.click(); toast('Exportado ' + name);
}

// --- Paleta de búsqueda (Ctrl+K)
const pages = [['resumen', 'Resumen'], ['servicios', 'Servicios'], ['pedidos', 'Pedidos'], ['actividad', 'Actividad'], ['errores', 'Errores'], ['sugerencias', 'Sugerencias'],
  ['entidades', 'Entidades'], ['pendientes', 'Pendientes'], ['usuarios', 'Usuarios'], ['correo', 'Correo'], ['ajustes', 'Ajustes']];
let palSel = 0, palItems = [];
function go(pg) { page = pg; $$('.nav').forEach(x => x.classList.toggle('on', x.dataset.page === pg)); render(); }
function openPalette() { if (!session) return; $('#palette').classList.add('open'); $('#palInput').value = ''; palFill(''); setTimeout(() => $('#palInput').focus(), 50); }
function palFill(q) {
  q = q.trim().toLowerCase(); palItems = [];
  pages.filter(([, n]) => !q || n.toLowerCase().includes(q)).forEach(([k, n]) => palItems.push({ t: n, k: 'página', fn: () => go(k) }));
  const num = q.replace(/\D/g, '');
  if (num) (state.orders || []).filter?.(o => String(o.id).startsWith(num)).slice(0, 6).forEach(o => palItems.push({ t: 'Pedido nº ' + o.id + ' · ' + (statusName[o.status] || o.status), k: 'pedido', fn: () => showOrder(o.id) }));
  if (q.length > 1) (state.orgs || []).filter?.(o => o.name.toLowerCase().includes(q)).slice(0, 5).forEach(o => palItems.push({ t: o.name, k: 'entidad', fn: () => { go('entidades'); } }));
  palSel = 0; drawPal();
}
function drawPal() { $('#palList').innerHTML = palItems.map((x, i) => '<div class="pal-item' + (i === palSel ? ' sel' : '') + '" data-i="' + i + '">' + esc(x.t) + '<small>' + x.k + '</small></div>').join('') || '<p class="empty">Sin resultados</p>';
  $$('.pal-item').forEach(e => e.onclick = () => { $('#palette').classList.remove('open'); palItems[+e.dataset.i].fn(); }); }
$('#palInput').oninput = e => palFill(e.target.value);
$('#palInput').onkeydown = e => {
  if (e.key === 'ArrowDown') { palSel = Math.min(palSel + 1, palItems.length - 1); drawPal(); e.preventDefault(); }
  else if (e.key === 'ArrowUp') { palSel = Math.max(palSel - 1, 0); drawPal(); e.preventDefault(); }
  else if (e.key === 'Enter' && palItems[palSel]) { $('#palette').classList.remove('open'); palItems[palSel].fn(); }
};
$('#palette').onclick = e => { if (e.target.id === 'palette') $('#palette').classList.remove('open'); };
document.addEventListener('keydown', e => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); openPalette(); }
  else if (e.key === 'Escape') { closeModal(); $('#palette').classList.remove('open'); }
  else if (e.key === 'F5') { e.preventDefault(); poll(); }
  else if (e.ctrlKey && /^[1-9]$/.test(e.key) && session) go(pages[+e.key - 1][0]);
});

// --- Detalle de un pedido
async function showOrder(id) {
  const o = (state.active || []).find?.(x => x.id === id) || (await api('/rest/v1/orders?id=eq.' + id + '&select=*,order_items(name,qty,detail,substitute,unavailable)'))[0];
  if (!o) return toast('Pedido no encontrado');
  const msgs = await api('/rest/v1/order_messages?order_id=eq.' + id + '&select=sender_role,body,created_at&order=id.desc&limit=6').catch(() => []);
  const org = (state.orgs || []).find?.(x => x.id === o.org_id);
  modal('Pedido nº ' + o.id, '<p><span class="status ' + (o.claimed_at ? 'reclamado' : o.status) + '">' + (o.claimed_at ? 'Reclamado' : statusName[o.status]) + '</span> · ' + fmtDate(o.created_at) + (org ? ' · ' + esc(org.name) : '') + '</p>' +
    '<p style="margin:8px 0;color:var(--muted)">' + esc(o.delivery_address) + '</p><table><tbody>' + (o.order_items || []).map(i => '<tr><td><b>' + i.qty + ' ×</b> ' + (i.unavailable ? '<s>' + esc(i.name) + '</s> (no había)' : esc(i.name) + (i.substitute ? ' → <b>' + esc(i.substitute) + '</b>' : '')) +
      (i.detail ? '<br><small>' + esc(i.detail) + '</small>' : '') + '</td></tr>').join('') + '</tbody></table>' +
    '<p style="margin-top:10px"><b>Total:</b> ' + eur(o.final_total || o.total) + ' · ' + esc(o.payment_method) + ' · ' + (o.payment_status === 'pagado' ? 'cobrado' : 'pendiente de cobro') + '</p>' +
    (msgs.length ? '<h3 style="margin-top:14px;font-size:16px">Últimos mensajes</h3>' + msgs.map(m => '<p style="font-size:13px"><b>' + esc(m.sender_role) + ':</b> ' + esc(m.body) + ' <small style="color:var(--muted)">' + ago(m.created_at) + '</small></p>').join('') : ''), []);
}

// --- Comprobar si hay versión nueva del programa (Releases «desktop-v…»)
async function checkDesktopUpdate() {
  if (document.documentElement.classList.contains('android')) return;   // en Android se actualiza con su propio aviso nativo
  if (state.updChecked && Date.now() - state.updChecked < 6 * 3600e3) return; state.updChecked = Date.now();
  try {
    const cur = await window.tlt.version(); const rel = await fetch('https://api.github.com/repos/' + REPO + '/releases?per_page=30').then(r => r.json());
    const d = (rel || []).filter(r => /^desktop-v/.test(r.tag_name)).map(r => ({ v: r.tag_name.slice(9), url: r.html_url }))[0];
    const newer = d && d.v.split('.').map(Number).some((x, i, a) => { const c = cur.split('.').map(Number); for (let j = 0; j < 3; j++) { if ((a[j] || 0) !== (c[j] || 0)) return (a[j] || 0) > (c[j] || 0); } return false; });
    if (newer) { const b = $('#updateBanner'); b.innerHTML = '⬆️ Hay una versión nueva del programa: <b>' + esc(d.v) + '</b><button class="btn small primary" style="background:#1B2428;color:#fff">Descargar</button><button class="btn small ghost" id="updClose" aria-label="Cerrar">✕</button>';
      setTimeout(() => { const c = document.getElementById('updClose'); if (c) c.onclick = () => { b.hidden = true; }; }, 0);
      b.hidden = false; b.querySelector('button').onclick = () => window.tlt.open(d.url); }
  } catch {}
}

// --- Páginas nuevas y páginas con acciones
function render() {
  const c = $('#content'), s = state;
  if (!s.svcs || !['pedidos', 'usuarios', 'correo', 'entidades', 'pendientes', 'servicios', 'actividad'].includes(page)) { renderBase(); afterBase(); return; }
  const orders = Array.isArray(s.orders) ? s.orders : [], active = Array.isArray(s.active) ? s.active : [];
  let h = '';
  if (page === 'servicios') {
    h = header('Servicios', 'Comprobación cada ' + prefs.interval + ' min · historial de las últimas 24 h') + '<div class="grid">' + s.svcs.map((v, i) =>
      '<div class="card svc" ' + stag(i) + (v.url ? ' data-url="' + esc(v.url) + '"' : '') + '><span class="dot ' + v.level + '"></span><div style="flex:1;min-width:0"><h3>' + esc(v.name) + '</h3><div class="s">' + esc(v.desc) + '</div><div class="m">' + esc(v.msg) + '</div>' + uptime(v.id) + spark(v.id) + '</div></div>').join('') + '</div>';
  } else if (page === 'pedidos') {
    const list = active.filter(o => (orderFilter === 'todos' || (orderFilter === 'reclamados' ? o.claimed_at : o.status === orderFilter)) &&
      (!orderQuery || String(o.id).includes(orderQuery) || (o.delivery_address || '').toLowerCase().includes(orderQuery.toLowerCase())));
    h = header('Pedidos en curso', active.length + ' pedidos sin terminar · toca uno para ver el detalle') +
      '<div class="toolbar">' + ['todos', 'pendiente', 'aceptado', 'comprando', 'en_camino', 'reclamados'].map(f => '<button class="chipf' + (orderFilter === f ? ' on' : '') + '" data-f="' + f + '">' + (f === 'todos' ? 'Todos' : f === 'reclamados' ? 'Reclamados' : statusName[f]) + '</button>').join('') +
      '<input id="oq" placeholder="Buscar nº o dirección" value="' + esc(orderQuery) + '"><button class="btn ghost small" id="expOrders">Exportar 30 días</button></div>' +
      (list.length ? '<table><thead><tr><th>Nº</th><th>Estado</th><th>Entidad</th><th>Hace</th><th>Dirección</th><th>Total</th></tr></thead><tbody>' + list.map((o, i) => {
        const org = (s.orgs || []).find?.(x => x.id === o.org_id); const late = o.status === 'pendiente' && Date.now() - new Date(o.created_at) > 2 * 3600e3;
        return '<tr class="order-row" data-o="' + o.id + '" ' + stag(i) + '><td><b>' + o.id + '</b></td><td><span class="status ' + (o.claimed_at ? 'reclamado' : o.status) + '">' + (o.claimed_at ? 'Reclamado' : statusName[o.status]) + '</span></td><td>' + esc(org?.name || '—') +
          '</td><td' + (late ? ' style="color:var(--bad);font-weight:700"' : '') + '>' + ago(o.created_at) + '</td><td>' + esc((o.delivery_address || '').slice(0, 48)) + '</td><td>' + eur(o.final_total || o.total) + '</td></tr>'; }).join('') + '</tbody></table>'
        : empty('Sin pedidos en curso', 'Cuando alguien pida, aparecerá aquí.'));
  } else if (page === 'actividad') {
    const byStatus = {}; orders.forEach(o => byStatus[o.status] = (byStatus[o.status] || 0) + 1);
    const cols = { pendiente: '#E0A100', aceptado: '#7BC9A4', comprando: '#5BB08A', en_camino: '#2E6B57', entregado: '#1F4D3F', cancelado: '#C2413B' };
    const total = orders.length || 1; let off = 0; const C = 2 * Math.PI * 50;
    const donut = '<div class="chart donut"><svg viewBox="0 0 120 120">' + Object.keys(cols).map(k => { const v = (byStatus[k] || 0) / total * C; const el = '<circle cx="60" cy="60" r="50" stroke="' + cols[k] + '" stroke-dasharray="' + v + ' ' + (C - v) + '" stroke-dashoffset="' + (-off) + '"/>'; off += v; return el; }).join('') +
      '</svg><div class="legend">' + Object.keys(cols).map(k => '<div><i style="background:' + cols[k] + '"></i>' + statusName[k] + ': <b>' + (byStatus[k] || 0) + '</b></div>').join('') + '</div></div>';
    const perOrg = (s.orgs || []).map?.(o => [o.name, orders.filter(x => x.org_id === o.id).length]).sort((a, b) => b[1] - a[1]).slice(0, 8) || [];
    const maxO = Math.max(1, ...perOrg.map(x => x[1]));
    const bars = '<div class="chart">' + perOrg.map(([n, v], i) => '<div style="margin:8px 0"><div style="display:flex;justify-content:space-between;font-size:13px"><span>' + esc(n) + '</span><b>' + v + '</b></div><div style="height:10px;border-radius:10px;background:var(--line);overflow:hidden"><div class="bar" style="height:100%;width:' + (v / maxO * 100) + '%;background:var(--pine);transform-origin:left;animation:barX .9s var(--spring) ' + (i * 60) + 'ms both"></div></div></div>').join('') + '</div>';
    const mrr = (s.orgs || []).filter?.(o => o.active && o.verification_status === 'verificada').reduce((a, o) => a + Number(o.price_month || 0), 0) || 0;
    h = header('Actividad', 'Últimos 30 días') + '<div class="grid"><div class="card kpi"><div class="n" data-to="' + orders.length + '">0</div><div class="l">Pedidos</div></div>' +
      '<div class="card kpi" ' + stag(1) + '><div class="n" data-to="' + Math.round(orders.filter(o => o.status === 'entregado').reduce((a, o) => a + Number(o.final_total || o.total || 0), 0)) + '">0</div><div class="l">€ en compras entregadas</div></div>' +
      '<div class="card kpi" ' + stag(2) + '><div class="n" data-to="' + Math.round(mrr) + '">0</div><div class="l">€/mes en cuotas (entidades activas)</div></div></div>' +
      '<h2 class="sec">Pedidos por día</h2>' + chart(orders) + '<div class="two" style="margin-top:16px"><div><h2 class="sec">Por estado</h2>' + donut + '</div><div><h2 class="sec">Por entidad</h2>' + bars + '</div></div>';
  } else if (page === 'entidades') {
    const orgs = Array.isArray(s.orgs) ? s.orgs : [];
    h = header('Entidades', orgs.length + ' entidades') + '<div class="toolbar"><button class="btn ghost small" id="expOrgs">Exportar a Excel</button></div><table><thead><tr><th>Entidad</th><th>Verificación</th><th>Cuota</th><th>Pagada hasta</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>' +
      orgs.map((o, i) => { const ver = o.verification_status === 'verificada' ? '<span class="pill ok">Verificada</span>' : o.verification_status === 'pendiente' ? '<span class="pill warn">En revisión</span>' : o.verification_status === 'rechazada' ? '<span class="pill bad">Rechazada</span>' : '<span class="pill">Sin datos</span>';
        const st = !o.active ? '<span class="pill bad">Desactivada</span>' : !o.accepting_orders ? '<span class="pill warn">En pausa</span>' : '<span class="pill ok">Activa</span>';
        return '<tr ' + stag(i) + '><td><b>' + esc(o.name) + '</b><br><small>' + esc(o.kind) + (o.tax_id ? ' · ' + esc(o.tax_id) : '') + '</small></td><td>' + ver + '</td><td>' + eur(o.price_month) + '</td><td>' + (o.subscription_until ? new Date(o.subscription_until).toLocaleDateString('es-ES') : '—') + '</td><td>' + st +
          '</td><td><div class="row-actions">' + (o.verification_status === 'pendiente' ? '<button class="btn primary" data-act="verify" data-id="' + o.id + '">Verificar</button><button class="btn ghost" data-act="reject" data-id="' + o.id + '">Rechazar</button>' : '') +
          '<button class="btn ghost" data-act="invite" data-id="' + o.id + '">Invitar</button><button class="btn ghost" data-act="month" data-id="' + o.id + '">+1 mes</button><button class="btn ghost" data-act="' + (o.active ? 'off' : 'on') + '" data-id="' + o.id + '">' + (o.active ? 'Desactivar' : 'Activar') + '</button>' +
          (o.active ? '<button class="btn ghost" data-act="' + (o.accepting_orders ? 'pause' : 'resume') + '" data-id="' + o.id + '">' + (o.accepting_orders ? 'Pausar' : 'Reanudar') + '</button>' : '') + '</div></td></tr>'; }).join('') + '</tbody></table>';
  } else if (page === 'pendientes') {
    const cs = Array.isArray(s.complaints) ? s.complaints : [], rs = Array.isArray(s.refunds) ? s.refunds : [];
    h = header('Pendientes', 'Responde reclamaciones y resuelve reembolsos desde aquí') + '<h2 class="sec">Hojas de reclamaciones (' + cs.length + ')</h2>' +
      (cs.length ? '<div class="list">' + cs.map((c, i) => '<div class="item" ' + stag(i) + '><span class="sev ' + (Date.now() - new Date(c.created_at) > 7 * 864e5 ? 'alta' : 'media') + '">' + esc(c.number || c.id) + '</span><div style="flex:1"><h4>' + esc(c.subject) + '</h4><p>' + fmtDate(c.created_at) + (c.order_id ? ' · pedido nº ' + c.order_id : '') + '</p></div><button class="btn primary small" data-cmp="' + c.id + '">Responder</button></div>').join('') + '</div>' : empty('Sin reclamaciones', 'Todas respondidas.')) +
      '<h2 class="sec">Reembolsos (' + rs.length + ')</h2>' + (rs.length ? '<div class="list">' + rs.map((r, i) => '<div class="item" ' + stag(i) + '><span class="sev media">' + eur(r.amount) + '</span><div style="flex:1"><h4>Pedido nº ' + r.order_id + '</h4><p>' + esc(r.reason) + ' · ' + fmtDate(r.created_at) + '</p></div><div class="row-actions"><button class="btn primary small" data-ref="' + r.id + '" data-ok="1">Aprobar</button><button class="btn ghost small" data-ref="' + r.id + '" data-ok="0">Denegar</button></div></div>').join('') + '</div>' : empty('Sin reembolsos', 'No hay solicitudes pendientes.'));
  } else if (page === 'usuarios') {
    h = header('Usuarios', 'Cuentas registradas') + (users ? (() => {
      const q = (state.uq || '').toLowerCase(), roles = {}; users.forEach(u => roles[u.role] = (roles[u.role] || 0) + 1);
      const list = users.filter(u => !q || (u.full_name || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q));
      return '<div class="grid">' + Object.entries(roles).map(([r, n], i) => '<div class="card kpi" ' + stag(i) + '><div class="n" data-to="' + n + '">0</div><div class="l">' + esc(r) + '</div></div>').join('') + '</div>' +
        '<div class="toolbar" style="margin-top:16px"><input id="uq" placeholder="Buscar nombre o correo" value="' + esc(state.uq || '') + '"><button class="btn ghost small" id="expUsers">Exportar</button></div>' +
        '<table><thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Teléfono</th></tr></thead><tbody>' + list.slice(0, 300).map((u, i) => '<tr ' + stag(Math.min(i, 20)) + '><td>' + esc(u.full_name) + '</td><td>' + esc(u.email) + '</td><td><span class="pill">' + esc(u.role) + '</span></td><td>' + esc(u.phone || '—') + '</td></tr>').join('') + '</tbody></table>'; })()
      : '<div class="grid">' + '<div class="skel"></div>'.repeat(4) + '</div>');
    if (!users) rpc('admin_users').then(u => { users = Array.isArray(u) ? u : []; if (page === 'usuarios') render(); }).catch(e => toast('No se pudieron cargar los usuarios: ' + e.message));
  } else if (page === 'correo') {
    const m = s.mail || {}; const last = m.last || [];
    h = header('Correo de compras', 'Brevo: pedidos recibidos y facturas al entregar') + '<div class="card" style="max-width:640px"><h3 style="font-family:Atkinson;font-size:16px">' + (m.db_key ? '✓ Clave de Brevo guardada' : '⚠️ Falta la clave de Brevo') + '</h3>' +
      '<p style="color:var(--muted);font-size:13px;margin:6px 0 12px">Brevo → ⚙ → SMTP y API → Claves API → «Generar una nueva clave API» (no la de MCP). Empieza por xkeysib-.</p>' +
      '<label>Clave API<input id="bkey" type="password" placeholder="xkeysib-…"></label><div class="row-actions"><button class="btn primary" id="saveKey">Guardar clave</button><button class="btn ghost" id="testMail">Enviar correo de prueba</button></div></div>' +
      '<h2 class="sec">Últimos envíos</h2>' + (last.length ? '<div class="list">' + last.map((r, i) => { const ok = r.status === 200 && /^enviado/.test(r.msg || '');
        return '<div class="item" ' + stag(i) + '><span class="sev ' + (ok ? 'baja' : 'media') + '">' + (ok ? 'enviado' : (r.status || '—')) + '</span><div><h4>' + esc(r.msg) + '</h4><p>' + fmtDate(r.at) + '</p></div></div>'; }).join('') + '</div>' : empty('Sin envíos todavía', 'Aparecerán al crear o entregar pedidos.'));
  }
  c.innerHTML = '<div class="page">' + h + '</div>'; afterBase();
}

function afterBase() {
  $$('.kpi .n').forEach(n => { if (n.textContent === '0' && +n.dataset.to) counter(n); });
  $$('[data-url]').forEach(e => { e.style.cursor = 'pointer'; e.onclick = () => window.tlt.open(e.dataset.url); });
  const rb = $('#refreshBtn'); if (rb) rb.onclick = poll;
  if (page === 'ajustes') bindSettings();
  $$('[data-f]').forEach(b => b.onclick = () => { orderFilter = b.dataset.f; render(); });
  const oq = $('#oq'); if (oq) { oq.oninput = e => { orderQuery = e.target.value; clearTimeout(oq._t); oq._t = setTimeout(() => { render(); $('#oq').focus(); $('#oq').setSelectionRange(99, 99); }, 300); }; }
  $$('.order-row').forEach(r => r.onclick = () => showOrder(+r.dataset.o));
  const eo = $('#expOrders'); if (eo) eo.onclick = () => csv('pedidos-30-dias.csv', [['Nº', 'Fecha', 'Estado', 'Entidad', 'Pago', 'Cobrado', 'Total']].concat((state.orders || []).map(o => [o.id, new Date(o.created_at).toLocaleString('es-ES'), statusName[o.status], ((state.orgs || []).find?.(x => x.id === o.org_id) || {}).name, o.payment_method, o.payment_status === 'pagado' ? 'Sí' : 'No', String(o.final_total || o.total).replace('.', ',')])));
  const eg = $('#expOrgs'); if (eg) eg.onclick = () => csv('entidades.csv', [['Entidad', 'Tipo', 'CIF/NIF', 'Verificación', 'Plan', 'Cuota', 'Pagada hasta', 'Activa']].concat((state.orgs || []).map(o => [o.name, o.kind, o.tax_id, o.verification_status, o.plan, o.price_month, o.subscription_until, o.active ? 'Sí' : 'No'])));
  const eu = $('#expUsers'); if (eu) eu.onclick = () => csv('usuarios.csv', [['Nombre', 'Correo', 'Rol', 'Teléfono']].concat((users || []).map(u => [u.full_name, u.email, u.role, u.phone])));
  const uq = $('#uq'); if (uq) uq.oninput = e => { state.uq = e.target.value; clearTimeout(uq._t); uq._t = setTimeout(() => { render(); $('#uq').focus(); $('#uq').setSelectionRange(99, 99); }, 300); };
  // Acciones de entidades
  $$('[data-act]').forEach(b => b.onclick = () => { const id = +b.dataset.id, o = state.orgs.find(x => x.id === id), act = b.dataset.act;
    const upd = (p, msg) => modal(msg.t, '<p>' + msg.d + '</p>', [{ t: msg.ok, fn: async () => { await rpc('update_organization', { p_org: id, p }); toast(msg.done); poll(); } }]);
    if (act === 'invite') modal('Invitar a ' + o.name, '<label>Correo<input id="mMail" type="email" placeholder="persona@ejemplo.com"></label><label>Nombre (opcional)<input id="mName"></label>' +
      '<label>Rol<select id="mRole" style="display:block;width:100%;font:inherit;padding:10px;border-radius:12px;border:2px solid var(--line);background:var(--bg);color:var(--ink);margin-top:6px"><option value="gestor">Administrador de la entidad</option><option value="repartidor">Repartidor</option><option value="cliente">Usuario</option></select></label>' +
      '<p style="color:var(--muted);font-size:13px">Le llegará un correo para crear su cuenta y su contraseña; al terminar ya estará dentro de la entidad.</p>',
      [{ t: 'Enviar invitación', fn: async () => { await rpc('send_invite_email', { p_org: id, p_role: $('#mRole').value, p_email: $('#mMail').value.trim(), p_name: $('#mName').value.trim() || null }); toast('Invitación enviada'); } }]);
    else if (act === 'verify') modal('Verificar ' + o.name, '<p>CIF/NIF: <b>' + esc(o.tax_id || '—') + '</b>. Compruébalo en la sede de la AEAT antes de verificar. El documento está en la app (Admin → Entidades → Ajustes).</p>', [{ t: 'Verificar', fn: async () => { await rpc('review_org_verification', { p_org: id, p_ok: true }); toast('Entidad verificada'); poll(); } }]);
    else if (act === 'reject') modal('Rechazar ' + o.name, '<label>Motivo<textarea id="mNote"></textarea></label>', [{ t: 'Rechazar', cls: 'ghost', fn: async () => { await rpc('review_org_verification', { p_org: id, p_ok: false, p_note: $('#mNote').value }); toast('Verificación rechazada'); poll(); } }]);
    else if (act === 'month') { const base = o.subscription_until && new Date(o.subscription_until) > new Date() ? new Date(o.subscription_until) : new Date(); base.setMonth(base.getMonth() + 1);
      upd({ subscription_until: base.toISOString().slice(0, 10), active: true }, { t: 'Renovar un mes', d: 'Pagada hasta el ' + base.toLocaleDateString('es-ES') + '.', ok: 'Renovar', done: 'Suscripción renovada' }); }
    else if (act === 'off' || act === 'on') upd({ active: act === 'on' }, { t: (act === 'on' ? 'Activar ' : 'Desactivar ') + o.name, d: act === 'on' ? 'Sus usuarios podrán volver a pedir.' : 'Sus usuarios no podrán pedir hasta que la actives.', ok: act === 'on' ? 'Activar' : 'Desactivar', done: 'Hecho' });
    else upd({ accepting_orders: act === 'resume' }, { t: (act === 'resume' ? 'Reanudar' : 'Pausar') + ' pedidos', d: o.name, ok: act === 'resume' ? 'Reanudar' : 'Pausar', done: 'Hecho' });
  });
  $$('[data-cmp]').forEach(b => b.onclick = () => modal('Responder reclamación', '<label>Respuesta<textarea id="mNote" placeholder="Explica la solución adoptada…"></textarea></label>', [{ t: 'Enviar respuesta', fn: async () => { await rpc('answer_complaint', { p_id: +b.dataset.cmp, p_response: $('#mNote').value }); toast('Respuesta guardada'); poll(); } }]));
  $$('[data-ref]').forEach(b => b.onclick = () => { const ok = b.dataset.ok === '1';
    modal(ok ? 'Aprobar reembolso' : 'Denegar reembolso', '<label>Nota (opcional)<textarea id="mNote"></textarea></label>', [{ t: ok ? 'Aprobar' : 'Denegar', cls: ok ? 'primary' : 'ghost', fn: async () => { await rpc('resolve_refund', { p_id: +b.dataset.ref, p_approve: ok, p_note: $('#mNote').value || null }); toast('Reembolso resuelto'); poll(); } }]); });
  const sk = $('#saveKey'); if (sk) sk.onclick = async () => { try { await rpc('set_brevo_key', { p_key: $('#bkey').value.trim() }); toast('Clave guardada'); poll(); } catch (e) { toast(e.message); } };
  const tm = $('#testMail'); if (tm) tm.onclick = async () => { try { await rpc('send_test_email'); toast('Enviando prueba…'); setTimeout(poll, 4000); } catch (e) { toast(e.message); } };
  const ordN = (state.active || []).length || 0; const bo = $('#badgeOrd'); if (bo) { bo.textContent = ordN; bo.classList.toggle('show', ordN > 0); }
}


// ======================= Móvil (Android) v2 =======================
const mobile = () => innerWidth <= 760;
const MAIN = ['resumen', 'servicios', 'pedidos', 'sugerencias'];
(function setupMobileNav() {
  const side = document.getElementById('side');
  side.querySelectorAll('.nav').forEach(n => { if (!MAIN.includes(n.dataset.page)) n.classList.add('extra'); });
  const more = document.createElement('button'); more.className = 'nav morebtn';
  more.innerHTML = '<svg viewBox="0 0 24 24"><path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/></svg><span>Más</span><b class="badge" id="badgeMore"></b>';
  side.insertBefore(more, side.querySelector('.side-sp'));
  window.openMore = () => {
    document.getElementById('moreList').innerHTML = [...side.querySelectorAll('.nav.extra')].map(n => '<button class="more-item" data-p="' + n.dataset.page + '">' + n.querySelector('svg').outerHTML +
      '<span>' + n.querySelector('span').textContent + '</span>' + (n.querySelector('.badge.show') ? '<b class="badge show">' + n.querySelector('.badge').textContent + '</b>' : '') + '</button>').join('');
    document.querySelectorAll('.more-item').forEach(b => b.onclick = () => { document.getElementById('more').classList.remove('open'); go(b.dataset.p); });
    document.getElementById('more').classList.add('open');
  };
  more.onclick = window.openMore;
  document.getElementById('more').onclick = e => { if (e.target.id === 'more') e.currentTarget.classList.remove('open'); };
  document.getElementById('mbSearch').onclick = () => openPalette();
  document.getElementById('mbRefresh').onclick = () => poll();
})();

// Título, estado y botón «Más» marcados según la página
const pageTitles = { resumen: 'Resumen', servicios: 'Servicios', pedidos: 'Pedidos en curso', actividad: 'Actividad', errores: 'Errores', sugerencias: 'Sugerencias',
  entidades: 'Entidades', pendientes: 'Pendientes', usuarios: 'Usuarios', correo: 'Correo de compras', ajustes: 'Ajustes' };
function mobileChrome() {
  const t = document.getElementById('mbTitle'); if (t) t.textContent = pageTitles[page] || 'T lo traigo';
  const st = document.getElementById('mbStatus'), tb = document.getElementById('tbStatus');
  if (st && tb) { st.className = tb.className.replace('tb-status', '').trim(); st.innerHTML = '<i></i>' + esc(tb.querySelector('span').textContent); }
  const mb = document.querySelector('.morebtn'); if (mb) { mb.onclick = window.openMore; mb.classList.toggle('on', !MAIN.includes(page)); document.querySelectorAll('#side .nav').forEach(n => { if (n !== mb && !MAIN.includes(n.dataset.page)) n.classList.remove('on'); }); }
  const extraBadges = [...document.querySelectorAll('#side .nav.extra .badge.show')].reduce((a, b) => a + (+b.textContent || 0), 0);
  const bm = document.getElementById('badgeMore'); if (bm) { bm.textContent = extraBadges; bm.classList.toggle('show', extraBadges > 0); }
  // Tablas → tarjetas: cada celda lleva el nombre de su columna
  document.querySelectorAll('#content table').forEach(tbl => { const ths = [...tbl.querySelectorAll('thead th')].map(x => x.textContent);
    tbl.classList.add('cards'); tbl.querySelectorAll('tbody tr').forEach(tr => [...tr.children].forEach((td, i) => td.setAttribute('data-label', ths[i] || ''))); });
  if (mobile()) document.querySelectorAll('#content .sec').forEach(h => { h.textContent = h.textContent.replace('últimos 14 días', 'últimos 7 días'); });
  // Versión real en Ajustes
  if (page === 'ajustes') window.tlt.version().then(v => { const p = [...document.querySelectorAll('#content p')].find(x => x.textContent.includes('Control 1.0')); if (p) p.textContent = p.textContent.replace('Control 1.0', 'Control ' + v); });
}
const _afterBase = afterBase; afterBase = function () { _afterBase(); mobileChrome(); };
const _poll = poll; poll = async function () { document.getElementById('mbRefresh')?.classList.add('spin'); try { await _poll(); } finally { document.getElementById('mbRefresh')?.classList.remove('spin'); mobileChrome(); } };

// Gráfica: en el móvil, 7 días con números más grandes
const _chart = chart; chart = function (orders) {
  if (!mobile()) return _chart(orders);
  const days = [...Array(7)].map((_, i) => { const d = new Date(); d.setHours(0, 0, 0, 0); d.setDate(d.getDate() - 6 + i); return d; });
  const counts = days.map(d => orders.filter(o => { const x = new Date(o.created_at); return x >= d && x < new Date(d.getTime() + 864e5); }).length);
  const max = Math.max(1, ...counts), W = 350, H = 150, bw = W / 7, dn = ['D', 'L', 'M', 'X', 'J', 'V', 'S'];
  return '<div class="chart"><svg viewBox="0 0 ' + W + ' ' + (H + 34) + '" style="height:auto">' + counts.map((n, i) => { const h = n / max * (H - 26);
    return '<rect class="bar" style="animation-delay:' + (i * 50) + 'ms" x="' + (i * bw + 8) + '" y="' + (H - h) + '" width="' + (bw - 16) + '" height="' + Math.max(h, 3) + '" rx="8" fill="' + (i === 6 ? '#F2B544' : '#2E6B57') + '"/>' +
      (n ? '<text x="' + (i * bw + bw / 2) + '" y="' + (H - h - 7) + '" text-anchor="middle" font-size="15" font-weight="700" fill="currentColor">' + n + '</text>' : '') +
      '<text x="' + (i * bw + bw / 2) + '" y="' + (H + 18) + '" text-anchor="middle" font-size="13" fill="#7A8A85">' + dn[days[i].getDay()] + '</text>' +
      '<text x="' + (i * bw + bw / 2) + '" y="' + (H + 32) + '" text-anchor="middle" font-size="10" fill="#9AA8A3">' + days[i].getDate() + '/' + (days[i].getMonth() + 1) + '</text>'; }).join('') + '</svg></div>';
};

// Tirar hacia abajo para actualizar
(function pullToRefresh() {
  const app = document.getElementById('app'), ptr = document.getElementById('ptr'); let y0 = null, dy = 0;
  app.addEventListener('touchstart', e => { y0 = app.scrollTop <= 0 ? e.touches[0].clientY : null; dy = 0; }, { passive: true });
  app.addEventListener('touchmove', e => { if (y0 == null) return; dy = Math.max(0, Math.min(120, e.touches[0].clientY - y0));
    ptr.style.opacity = String(dy / 80); ptr.style.transform = 'translateY(' + (dy / 2 - 40) + 'px) rotate(' + dy * 3 + 'deg)'; }, { passive: true });
  app.addEventListener('touchend', async () => { if (y0 == null) return; y0 = null;
    if (dy > 80) { ptr.classList.add('go'); await poll(); ptr.classList.remove('go'); }
    ptr.style.transition = 'all .3s'; ptr.style.opacity = '0'; ptr.style.transform = 'translateY(-40px)'; setTimeout(() => ptr.style.transition = '', 300); });
})();
