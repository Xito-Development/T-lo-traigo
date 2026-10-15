// T lo traigo Control — proceso principal (Electron)
const { app, BrowserWindow, ipcMain, Tray, Menu, Notification, safeStorage, nativeImage, shell } = require('electron');
const path = require('path'); const fs = require('fs'); const tls = require('tls');

app.setAppUserModelId('es.unocerobits.tlotraigo.control');
let win, tray, quitting = false;
const store = path.join(app.getPath('userData'), 'sesion.bin');
const prefsFile = path.join(app.getPath('userData'), 'ajustes.json');

function createWindow() {
  win = new BrowserWindow({
    width: 1280, height: 820, minWidth: 980, minHeight: 640, frame: false, backgroundColor: '#1F4D3F', show: false,
    icon: path.join(__dirname, 'renderer', 'logo.png'),
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false, sandbox: true },
  });
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  win.once('ready-to-show', () => win.show());
  win.on('close', e => { if (!quitting && readPrefs().tray !== false) { e.preventDefault(); win.hide(); } });   // se queda vigilando en la bandeja
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: 'deny' }; });
}

function readPrefs() { try { return JSON.parse(fs.readFileSync(prefsFile, 'utf8')); } catch { return {}; } }

function createTray() {
  tray = new Tray(nativeImage.createFromPath(path.join(__dirname, 'renderer', 'logo.png')).resize({ width: 16, height: 16 }));
  tray.setToolTip('T lo traigo Control');
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: 'Abrir el panel', click: () => { win.show(); win.focus(); } },
    { type: 'separator' },
    { label: 'Salir', click: () => { quitting = true; app.quit(); } },
  ]));
  tray.on('click', () => { win.isVisible() ? win.focus() : win.show(); });
}

if (!app.requestSingleInstanceLock()) app.quit();
else {
  app.on('second-instance', () => { if (win) { win.show(); win.focus(); } });
  app.whenReady().then(() => { createWindow(); createTray(); });
  app.on('before-quit', () => { quitting = true; });
}

// ---------- Ventana sin marco: botones propios ----------
ipcMain.on('win', (_e, a) => { if (a === 'min') win.minimize(); else if (a === 'max') win.isMaximized() ? win.unmaximize() : win.maximize(); else if (a === 'close') win.close(); });

// ---------- Sesión cifrada con la protección de Windows (DPAPI) ----------
ipcMain.handle('session:save', (_e, data) => {
  const raw = JSON.stringify(data);
  fs.writeFileSync(store, safeStorage.isEncryptionAvailable() ? safeStorage.encryptString(raw) : Buffer.from(raw));
  return true;
});
ipcMain.handle('session:load', () => {
  try { const b = fs.readFileSync(store); return JSON.parse(safeStorage.isEncryptionAvailable() ? safeStorage.decryptString(b) : b.toString()); } catch { return null; }
});
ipcMain.handle('session:clear', () => { try { fs.unlinkSync(store); } catch {} return true; });

// ---------- Ajustes ----------
ipcMain.handle('prefs:get', () => readPrefs());
ipcMain.handle('prefs:set', (_e, p) => {
  fs.writeFileSync(prefsFile, JSON.stringify(p));
  app.setLoginItemSettings({ openAtLogin: !!p.autostart, args: ['--oculto'] });
  return true;
});

// ---------- Comprobaciones que el navegador no puede hacer ----------
ipcMain.handle('check:ssl', (_e, host) => new Promise(resolve => {
  const s = tls.connect({ host, port: 443, servername: host, timeout: 8000 }, () => {
    const c = s.getPeerCertificate(); s.end();
    resolve({ ok: true, validTo: c.valid_to, issuer: c.issuer && (c.issuer.O || c.issuer.CN), authorized: s.authorized });
  });
  s.on('error', err => resolve({ ok: false, error: err.message }));
  s.on('timeout', () => { s.destroy(); resolve({ ok: false, error: 'Sin respuesta' }); });
}));
ipcMain.handle('check:http', async (_e, url, opts = {}) => {
  const t0 = Date.now();
  try {
    const ctrl = new AbortController(); const to = setTimeout(() => ctrl.abort(), 10000);
    const r = await fetch(url, { method: opts.method || 'GET', headers: opts.headers || {}, signal: ctrl.signal });
    clearTimeout(to);
    return { ok: true, status: r.status, ms: Date.now() - t0 };
  } catch (e) { return { ok: false, error: e.name === 'AbortError' ? 'Tiempo de espera agotado' : e.message, ms: Date.now() - t0 }; }
});

// ---------- Avisos de Windows ----------
ipcMain.on('notify', (_e, title, body) => {
  const n = new Notification({ title, body, icon: path.join(__dirname, 'renderer', 'logo.png') });
  n.on('click', () => { win.show(); win.focus(); }); n.show();
});
ipcMain.on('tray:status', (_e, level, text) => { if (tray) tray.setToolTip(`T lo traigo Control — ${text}`); });
ipcMain.on('open', (_e, url) => { if (/^https:\/\//.test(url)) shell.openExternal(url); });
