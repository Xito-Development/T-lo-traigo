<#
  Vigila la carpeta Descargas y aplica al proyecto los archivos de T lo traigo que descargues de Claude:
   - tlotraigo*.zip        -> se descomprime encima del proyecto (respeta .git, build y local.properties)
   - web-t-lo-traigo*.zip  -> se copia en web\t-lo-traigo
   - CLAUDE*.md            -> se copia como CLAUDE.md en la raíz
  Si hay cambios sin guardar en Git, primero los aparta con "git stash" para no perder nada.
  Los archivos ya aplicados se mueven a .descargas\procesados y queda registro en .descargas\registro.txt
  Se instala para que arranque solo con: instalar-vigilante.bat
#>
param(
  [string]$Proyecto = (Split-Path $PSScriptRoot -Parent),
  [string]$Descargas = (New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path
)
$ErrorActionPreference = 'Continue'
$mutex = New-Object Threading.Mutex($false, 'Global\TLoTraigoVigilante')
if (-not $mutex.WaitOne(0)) { exit }   # ya hay uno en marcha

$Base = Join-Path $Proyecto '.descargas'
$Hechos = Join-Path $Base 'procesados'
New-Item -ItemType Directory -Force $Hechos | Out-Null
$Log = Join-Path $Base 'registro.txt'
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

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
function GuardarCambiosGit($motivo) {
  if (-not (Test-Path (Join-Path $Proyecto '.git')) -or -not (Get-Command git -ErrorAction SilentlyContinue)) { return $null }
  Push-Location $Proyecto
  $sucio = git status --porcelain 2>$null
  if ($sucio) { git stash push -u -m "Antes de importar $motivo" 2>$null | Out-Null; Pop-Location; return "Tus cambios sin guardar se han apartado (git stash list)." }
  Pop-Location; return $null
}
function Copiar($origen, $destino) {
  robocopy $origen $destino /E /XD .git build .gradle .kotlin .descargas /XF local.properties /R:1 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
  return ($LASTEXITCODE -lt 8)
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
  $aviso = GuardarCambiosGit $nombre
  $ok = $false
  try {
    if ($tipo -eq 'claude') {
      Copy-Item $ruta (Join-Path $Proyecto 'CLAUDE.md') -Force; $ok = $true
    } else {
      $tmp = Join-Path $env:TEMP ("tlt_" + [guid]::NewGuid())
      Expand-Archive -Path $ruta -DestinationPath $tmp -Force
      if ($tipo -eq 'proyecto') {
        $raiz = Get-ChildItem $tmp -Recurse -Filter 'settings.gradle.kts' | Select-Object -First 1
        if ($raiz) { $ok = Copiar $raiz.DirectoryName $Proyecto }
      } else {
        $web = Get-ChildItem $tmp -Recurse -Filter 'index.html' | Select-Object -First 1
        if ($web) { $ok = Copiar $web.DirectoryName (Join-Path $Proyecto 'web\t-lo-traigo') }
      }
      Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
  } catch { Registrar "Error con ${nombre}: $_" }
  if ($ok) {
    $dest = Join-Path $Hechos ((Get-Date -Format 'yyyyMMdd-HHmmss') + '_' + $nombre)
    Move-Item $ruta $dest -Force
    Registrar "Aplicado $nombre ($tipo). $aviso"
    Avisar ("$nombre aplicado al proyecto." + $(if ($aviso) { " $aviso" } else { '' }))
  } else {
    Registrar "No se aplicó $nombre"; Avisar "No se pudo aplicar $nombre. Revisa .descargas\registro.txt" 'Warning'
  }
}

Registrar "Vigilando $Descargas -> $Proyecto"
Get-ChildItem $Descargas -File | ForEach-Object { Procesar $_.FullName }   # pendientes de antes

$w = New-Object IO.FileSystemWatcher $Descargas
$w.IncludeSubdirectories = $false; $w.EnableRaisingEvents = $true
Register-ObjectEvent $w Created -SourceIdentifier tltCreado | Out-Null
Register-ObjectEvent $w Renamed -SourceIdentifier tltRenombrado | Out-Null   # Chrome/Edge: .crdownload -> .zip
while ($true) {
  $e = Wait-Event
  $ruta = $e.SourceEventArgs.FullPath
  Remove-Event -EventIdentifier $e.EventIdentifier
  Start-Sleep -Milliseconds 800
  Procesar $ruta
}
