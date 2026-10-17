// En Android, el panel usa el puente nativo AndroidTLT con la misma interfaz que el preload de Windows (window.tlt).
(function () {
  if (window.tlt || !window.AndroidTLT) return;
  const A = window.AndroidTLT, waits = {}; let n = 0;
  window.__tltCb = (id, res) => { const f = waits[id]; delete waits[id]; if (f) f(res); };
  const call = (fn, ...args) => new Promise(res => { const id = 'c' + (++n); waits[id] = res; A[fn](id, ...args); });
  window.tlt = {
    win: a => A.win(a),
    session: { save: async d => A.sessionSave(JSON.stringify(d)), load: async () => { const s = A.sessionLoad(); return s ? JSON.parse(s) : null; }, clear: async () => A.sessionClear() },
    prefs: { get: async () => JSON.parse(A.prefsGet() || '{}'), set: async p => A.prefsSet(JSON.stringify(p)) },
    ssl: host => call('ssl', host),
    http: (url, opts) => call('http', url, JSON.stringify(opts || {})),
    notify: (t, b) => A.notify(t, b), trayStatus: () => {}, open: url => A.open(url), version: async () => A.version(),
  };
  document.documentElement.classList.add('android');
  const fitViewport = () => { const m = document.querySelector('meta[name=viewport]'); const w = screen.width || innerWidth;
    if (m) m.setAttribute('content', w > 760 ? 'width=740, viewport-fit=cover, user-scalable=no' : 'width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no'); };
  fitViewport(); addEventListener('orientationchange', () => setTimeout(fitViewport, 200));
  // Botón atrás de Android: cierra ventanas abiertas o vuelve al resumen
  window.__tltBack = () => {
    const m = document.querySelector('.modal.open'); if (m) { m.classList.remove('open'); return true; }
    try { if (typeof page !== 'undefined' && page !== 'resumen' && document.getElementById('app').classList.contains('show')) { go('resumen'); return true; } } catch (e) {}
    return false;
  };
})();
