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
function schedule() { clearTimeout(timer); timer = setTimeout(poll, prefs.interval * 60000); }

async function poll() {
  $('.spin')?.classList.add('on');
  try { await collect(); } catch (e) { console.error(e); }
  evaluate(); render(); notifyChanges(); schedule();
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

function render() {
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
      sw('setNotify', 'Avisos de Windows cuando algo falle', prefs.notify) + sw('setAuto', 'Iniciar con Windows (en la bandeja)', prefs.autostart) + sw('setTray', 'Al cerrar, seguir vigilando en la bandeja', prefs.tray !== false) + sw('setDark', 'Modo oscuro', prefs.dark) +
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
