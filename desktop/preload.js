const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('tlt', {
  win: a => ipcRenderer.send('win', a),
  session: { save: d => ipcRenderer.invoke('session:save', d), load: () => ipcRenderer.invoke('session:load'), clear: () => ipcRenderer.invoke('session:clear') },
  prefs: { get: () => ipcRenderer.invoke('prefs:get'), set: p => ipcRenderer.invoke('prefs:set', p) },
  ssl: host => ipcRenderer.invoke('check:ssl', host),
  http: (url, opts) => ipcRenderer.invoke('check:http', url, opts),
  notify: (t, b) => ipcRenderer.send('notify', t, b),
  trayStatus: (l, t) => ipcRenderer.send('tray:status', l, t),
  open: url => ipcRenderer.send('open', url),
  version: () => ipcRenderer.invoke('app:version'),
});
