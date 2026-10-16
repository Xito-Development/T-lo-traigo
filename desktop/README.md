# T lo traigo Control (Windows 10/11)

Programa de escritorio para el **superadministrador**: estado de todos los servicios (base de datos, inicio de sesión,
funciones, web, certificado SSL, compilación y versión de la app), actividad, errores, sugerencias, entidades,
reclamaciones y reembolsos pendientes. Avisos de Windows cuando algo falla y sigue vigilando en la bandeja.

**1.1:** pedidos en curso, usuarios, correo (Brevo), acciones sobre entidades, reclamaciones y reembolsos, Ctrl+K para buscar, Ctrl+1…9 para cambiar de página, F5 para comprobar, historial de 24 h, exportar a Excel y aviso de versión nueva.

- **Instalar:** descarga `TLoTraigo-Control-Setup-X.Y.Z.exe` de la Release `desktop-vX.Y.Z` en GitHub y ábrelo.
  Windows SmartScreen puede avisar porque el programa no está firmado: «Más información» → «Ejecutar de todas formas».
- **Primera vez:** asistente con bienvenida → acceso con la cuenta de superadministrador → preferencias → listo.
- **Credenciales:** se guardan cifradas con la protección de Windows (DPAPI), solo en ese equipo.
- **Compilar:** lo hace GitHub Actions (`.github/workflows/desktop.yml`) al subir cambios en `desktop/`. Para una versión
  nueva, sube el número de `version` en `desktop/package.json`.
- **Probar en local:** `cd desktop && npm install && npm start`.
