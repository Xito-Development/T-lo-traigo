<#
  Vigilante de Descargas de T lo traigo (se ejecuta oculto al iniciar sesión).
  Lee la configuración de %LOCALAPPDATA%\TLoTraigo\vigilante.json:
    Proyecto     carpeta del proyecto (se cambia con cambiar-ubicacion.bat)
    Descargas    carpeta vigilada
    ConservarGit true = mantiene la carpeta .git (historial y conexión con GitHub)
    Copias       nº de copias de seguridad que se guardan antes de reemplazar
  Al descargar:
    tlotraigo*.zip        -> REEMPLAZA el proyecto por el contenido del ZIP (lo que no esté en el ZIP se elimina)
    web-t-lo-traigo*.zip  -> REEMPLAZA web\t-lo-traigo
    CLAUDE*.md            -> se copia como CLAUDE.md
  Antes de reemplazar guarda una copia en %LOCALAPPDATA%\TLoTraigo\copias\
#>
$ErrorActionPreference = 'Continue'
$App = Join-Path $env:LOCALAPPDATA 'TLoTraigo'
$CfgFile = Join-Path $App 'vigilante.json'
$mutex = New-Object Threading.Mutex($false, 'Global\TLoTraigoVigilante')
if (-not $mutex.WaitOne(0)) { exit }
New-Item -ItemType Directory -Force (Join-Path $App 'procesados'), (Join-Path $App 'copias') | Out-Null
$Log = Join-Path $App 'registro.txt'
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

function Cfg {
  $c = Get-Content $CfgFile -Raw -Encoding utf8 | ConvertFrom-Json
  if (-not $c.Copias) { $c | Add-Member Copias 5 -Force }
  return $c
}
function Registrar($t) { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $t" | Add-Content $Log -Encoding utf8 }
function Avisar($t, $tipo = 'Info') {
  $n = New-Object System.Windows.Forms.NotifyIcon
  $n.Icon = [System.Drawing.SystemIcons]::Information; $n.Visible = $true
  $n.ShowBalloonTip(6000, 'T lo traigo', $t, $tipo); Start-Sleep 7; $n.Dispose()
}
function EsperarDescarga($ruta) {
  for ($i = 0; $i -lt 120; $i++) {
    try { $s = [IO.File]::Open($ruta, 'Open', 'Read', 'None'); $s.Close(); Start-Sleep 1; return $true } catch { Start-Sleep 1 }
  }
  return $false
}
function CopiaSeguridad($carpeta, $c) {
  if (-not (Test-Path $carpeta) -or -not (Get-ChildItem $carpeta -Force | Select-Object -First 1)) { return }
  $dest = Join-Path $App ("copias\" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '_' + (Split-Path $carpeta -Leaf))
  robocopy $carpeta $dest /E /XD .git build .gradle .kotlin .idea /R:1 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
  Get-ChildItem (Join-Path $App 'copias') -Directory | Sort-Object Name -Descending | Select-Object -Skip ([int]$c.Copias) |
    ForEach-Object { Remove-Item $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }
}
# Reemplaza el contenido de $destino por $origen (equivale a borrar la carpeta y descomprimir de nuevo).
# Se conserva la carpeta raíz (por si está abierta en Claude Code), local.properties, cachés de compilación y, si se elige, .git.
function Reemplazar($origen, $destino, $c) {
  New-Item -ItemType Directory -Force $destino | Out-Null
  $xd = @('build', '.gradle', '.kotlin', '.idea'); if ($c.ConservarGit) { $xd += '.git' }
  # version.properties, CHANGELOG.md y las notas de Play los gestiona publicar.bat: se conservan los de tu carpeta
  $propios = @('local.properties', 'version.properties', 'CHANGELOG.md', 'default.txt')
  robocopy $origen $destino /MIR /XD $xd /XF $propios /R:2 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
  $ok = ($LASTEXITCODE -lt 8)
  foreach ($f in @('version.properties', 'CHANGELOG.md', 'fastlane\metadata\android\es-ES\changelogs\default.txt')) {   # carpeta nueva: copiarlos la primera vez
    $o = Join-Path $origen $f; $d = Join-Path $destino $f
    if ((Test-Path $o) -and -not (Test-Path $d)) { New-Item -ItemType Directory -Force (Split-Path $d) | Out-Null; Copy-Item $o $d }
  }
  return $ok
}

function Procesar($ruta) {
  $nombre = Split-Path $ruta -Leaf
  $tipo = switch -Regex ($nombre) {
    '^web-t-lo-traigo.*\.zip$' { 'web'; break }
    '^tlotraigo.*\.zip$'       { 'proyecto'; break }
    '^CLAUDE.*\.md$'           { 'claude'; break }
    default                    { $null }
  }
  if (-not $tipo -or -not (Test-Path $ruta)) { return }
  if (-not (EsperarDescarga $ruta)) { Registrar "No se pudo abrir $nombre"; return }
  $c = Cfg; $P = $c.Proyecto
  $ok = $false
  try {
    if ($tipo -eq 'claude') {
      New-Item -ItemType Directory -Force $P | Out-Null
      Copy-Item $ruta (Join-Path $P 'CLAUDE.md') -Force; $ok = $true
    } else {
      $tmp = Join-Path $env:TEMP ("tlt_" + [guid]::NewGuid())
      Expand-Archive -Path $ruta -DestinationPath $tmp -Force
      if ($tipo -eq 'proyecto') {
        $raiz = Get-ChildItem $tmp -Recurse -Filter 'settings.gradle.kts' | Select-Object -First 1
        if ($raiz) {
          CopiaSeguridad $P $c; $ok = Reemplazar $raiz.DirectoryName $P $c
          # Se actualiza a sí mismo para el próximo inicio de sesión si el ZIP trae una versión nueva del vigilante
          $nuevo = Join-Path $P 'scripts\vigilar-descargas.ps1'
          if ($ok -and (Test-Path $nuevo)) { Copy-Item $nuevo (Join-Path $App 'vigilar-descargas.ps1') -Force }
        }
      } else {
        $web = Get-ChildItem $tmp -Recurse -Filter 'index.html' | Select-Object -First 1
        $dw = Join-Path $P 'web\t-lo-traigo'
        if ($web) { CopiaSeguridad $dw $c; $ok = Reemplazar $web.DirectoryName $dw $c }
      }
      Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
  } catch { Registrar "Error con ${nombre}: $_" }
  if ($ok) {
    Move-Item $ruta (Join-Path $App ('procesados\' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '_' + $nombre)) -Force
    Registrar "Aplicado $nombre ($tipo) en $P"
    $pub = Join-Path $P 'scripts\publicar.ps1'
    if ($tipo -eq 'proyecto' -and $c.AutoPublicar -and (Test-Path $pub)) {
      Registrar 'Publicando automáticamente'
      Start-Process powershell.exe -WindowStyle Hidden -WorkingDirectory $P -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$pub`" -Auto"
    } else { Avisar "$nombre aplicado en $P" }
  } else {
    Registrar "No se aplicó $nombre"; Avisar "No se pudo aplicar $nombre. Mira $Log" 'Warning'
  }
}

$c0 = Cfg
Registrar "Vigilando $($c0.Descargas) -> $($c0.Proyecto)"
Get-ChildItem $c0.Descargas -File | ForEach-Object { Procesar $_.FullName }
$w = New-Object IO.FileSystemWatcher $c0.Descargas
$w.IncludeSubdirectories = $false; $w.EnableRaisingEvents = $true
Register-ObjectEvent $w Created -SourceIdentifier tltCreado | Out-Null
Register-ObjectEvent $w Renamed -SourceIdentifier tltRenombrado | Out-Null
while ($true) {
  $e = Wait-Event
  $ruta = $e.SourceEventArgs.FullPath
  Remove-Event -EventIdentifier $e.EventIdentifier
  Start-Sleep -Milliseconds 800
  Procesar $ruta
}
