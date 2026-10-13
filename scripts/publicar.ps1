<#
  Publica T lo traigo con un solo paso (Windows).
  Uso: doble clic en "publicar.bat" en la raíz del proyecto.
  Opciones: .\scripts\publicar.ps1 [-Tipo parche|menor|mayor] [-Mensaje "qué ha cambiado"] [-SoloSubir]

  Qué hace:
   1. Instala Git y GitHub CLI si faltan e inicia sesión en GitHub (solo la primera vez).
   2. Conecta la carpeta con https://github.com/Xito-Development/T-lo-traigo
   3. Sube la versión (0.4.0 -> 0.4.1) y añade tu mensaje al CHANGELOG.
   4. Hace commit y push. GitHub Actions compila y crea la Release con el APK automáticamente.
#>
param(
  [ValidateSet('parche','menor','mayor')][string]$Tipo = 'parche',
  [string]$Mensaje = '',
  [switch]$SoloSubir,  # sube los cambios sin crear versión nueva
  [switch]$Auto        # sin preguntas (lo usa el vigilante); avisa con una notificación al terminar
)
$ErrorActionPreference = 'Continue'   # git escribe avisos por stderr; los errores se comprueban con LASTEXITCODE
$PendienteVacio = "# Cambios pendientes de publicar`n`n<!-- Lo que pongas debajo (### Añadido / ### Cambiado / ### Corregido y líneas '- ') serán las notas de la próxima versión. publicar.bat lo vacía al publicar. -->`n"
function Escribir($ruta, $texto) { [IO.File]::WriteAllText((Join-Path (Get-Location) $ruta), $texto, (New-Object Text.UTF8Encoding $false)) }  # UTF-8 sin BOM
$Org = 'Xito-Development'; $Name = 'T-lo-traigo'
$RepoUrl = "https://github.com/$Org/$Name.git"
Set-Location (Split-Path $PSScriptRoot -Parent)

function Aviso($t, $tipo = 'Info') {
  Add-Type -AssemblyName System.Windows.Forms, System.Drawing
  $n = New-Object System.Windows.Forms.NotifyIcon; $n.Icon = [System.Drawing.SystemIcons]::Information; $n.Visible = $true
  $n.ShowBalloonTip(8000, 'T lo traigo', $t, $tipo); Start-Sleep 9; $n.Dispose()
}
function Paso($t) { Write-Host "`n==> $t" -ForegroundColor Green }
function Asegurar($cmd, $wingetId) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    Paso "Instalando $cmd"
    winget install --id $wingetId -e --silent --accept-source-agreements --accept-package-agreements | Out-Null
    $env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { throw "No se pudo instalar $cmd. Instálalo a mano y vuelve a ejecutar." }
  }
}

Paso 'Comprobando herramientas'
Asegurar git 'Git.Git'
Asegurar gh 'GitHub.cli'
gh auth status *> $null
if ($LASTEXITCODE -ne 0 -and $Auto) { Aviso 'Para publicar solo, ejecuta una vez publicar.bat e inicia sesión en GitHub.' 'Warning'; return }
if ($LASTEXITCODE -ne 0) { Paso 'Inicia sesión en GitHub (se abre el navegador)'; gh auth login --web --git-protocol https --hostname github.com }
gh auth setup-git | Out-Null
if (-not (git config user.name))  { git config --global user.name  (gh api user --jq .login) }
if (-not (git config user.email)) { git config --global user.email ((gh api user --jq .id) + '+' + (gh api user --jq .login) + '@users.noreply.github.com') }

Paso "Conectando con $Org/$Name"
if (-not (Test-Path .git)) { git init -b main | Out-Null }
$remote = (git remote get-url origin 2>$null)
if (-not $remote) { git remote add origin $RepoUrl } elseif ($remote -ne $RepoUrl) { git remote set-url origin $RepoUrl }
git fetch origin --tags --quiet 2>$null
$hayRemoto = (git ls-remote --heads origin main) -ne $null
$hayLocal  = (git rev-parse --verify -q HEAD 2>$null) -ne $null
if ($hayRemoto -and -not $hayLocal) { git fetch origin main --quiet; git reset --soft origin/main | Out-Null }

# --- Versión ---
# La siguiente versión parte de la MAYOR entre version.properties y los tags ya publicados en GitHub,
# así nunca choca con una versión existente aunque el proyecto venga de un ZIP más antiguo.
function V($t) { try { [version](($t -split '-')[0]) } catch { [version]'0.0.0' } }
if (Test-Path CAMBIOS_PENDIENTES.md) {
  $m = [regex]::Match((Get-Content CAMBIOS_PENDIENTES.md -Raw -Encoding utf8), '(?im)^\s*tipo:\s*(parche|menor|mayor)')
  if ($m.Success) { $Tipo = $m.Groups[1].Value.ToLower() }
}
$props = Get-Content version.properties -Raw -Encoding utf8
$actual = ([regex]'VERSION_NAME=(\S+)').Match($props).Groups[1].Value
$tags = @(git ls-remote --tags origin 'refs/tags/v*' 2>$null | ForEach-Object { ($_ -split 'refs/tags/v')[1] } | Where-Object { $_ -and $_ -notmatch '\^\{\}' })
$mayorTag = $tags | Sort-Object { V $_ } -Descending | Select-Object -First 1
$base = if ($mayorTag -and (V $mayorTag) -ge (V $actual)) { $mayorTag } else { $actual }
if ($SoloSubir) { $nueva = $actual }
elseif ($tags -contains $base) {
  $p = V $base
  $nueva = switch ($Tipo) { 'mayor' { "$($p.Major + 1).0.0" } 'menor' { "$($p.Major).$($p.Minor + 1).0" } default { "$($p.Major).$($p.Minor).$($p.Build + 1)" } }
} else { $nueva = $base }
if ($mayorTag) { Write-Host "Última versión publicada: $mayorTag  ->  nueva: $nueva" }

if (-not $SoloSubir) {
  # Notas: CAMBIOS_PENDIENTES.md (lo rellena Claude) y/o tu mensaje
  $pend = ''
  if (Test-Path CAMBIOS_PENDIENTES.md) {
    $pend = ((Get-Content CAMBIOS_PENDIENTES.md -Raw -Encoding utf8) -split "`n" | Where-Object { $_ -match '^\s*(###|- )' }) -join "`n"
  }
  if ($Auto -and -not $Mensaje -and -not $pend) { $Mensaje = 'Mejoras y correcciones.' }
  if (-not $Mensaje -and -not $pend) { $Mensaje = Read-Host "¿Qué ha cambiado en la versión $nueva? (una frase)" }
  if (-not $Mensaje -and -not $pend) { $Mensaje = 'Mejoras y correcciones.' }
  $cuerpo = if ($pend) { $pend.Trim() } else { "### Cambiado`n- $Mensaje" }
  if ($pend -and $Mensaje) { $cuerpo += "`n- $Mensaje" }
  if (-not $Mensaje) { $Mensaje = (($pend -split "`n" | Where-Object { $_ -match '^- ' } | Select-Object -First 1) -replace '^- ', '') }

  Escribir version.properties "VERSION_NAME=$nueva"
  $log = Get-Content CHANGELOG.md -Raw -Encoding utf8
  if ($log -notmatch [regex]::Escape("## [$nueva]")) {
    $entrada = "## [$nueva] - $(Get-Date -Format 'yyyy-MM-dd')`n$cuerpo`n`n"
    $i = $log.IndexOf('## [')
    $log = if ($i -ge 0) { $log.Insert($i, $entrada) } else { $log + "`n" + $entrada }
    Escribir CHANGELOG.md $log
  }
  $nota = ($cuerpo -split "`n" | Where-Object { $_ -match '^- ' } | ForEach-Object { $_ -replace '^- ', '• ' }) -join "`n"
  if ($nota.Length -gt 480) { $nota = $nota.Substring(0, 480) }
  Escribir fastlane/metadata/android/es-ES/changelogs/default.txt "$nota`n"
  if (Test-Path CAMBIOS_PENDIENTES.md) { Escribir CAMBIOS_PENDIENTES.md $PendienteVacio }
}

Paso 'Subiendo a GitHub'
git add -A
$titulo = if ($SoloSubir) { 'Actualización' } else { "Versión ${nueva}: $Mensaje" }
git diff --cached --quiet; if ($LASTEXITCODE -ne 0) { git commit -m $titulo | Out-Null }
git push -u origin main
if ($LASTEXITCODE -ne 0) { throw 'No se pudo subir. Revisa que tienes permisos en la organización.' }

Paso 'Listo'
$web = "https://github.com/$Org/$Name/releases/latest"
if ($Auto) {
  Aviso "Publicando la versión $nueva. Tarda unos 10 minutos; te aviso al terminar."
  Start-Sleep -Seconds 20
  $id = gh run list -R "$Org/$Name" -b main -L 1 --json databaseId -q '.[0].databaseId' 2>$null
  if ($id) { gh run watch $id -R "$Org/$Name" --exit-status *> $null }
  if ($id -and $LASTEXITCODE -eq 0) { Aviso "¡Versión $nueva publicada! Los móviles la recibirán al abrir la app." }
  else { Aviso "La publicación de la $nueva ha fallado. Abre github.com/$Org/$Name/actions o pregunta a Claude." 'Warning' }
  return
}
if ($SoloSubir) { Write-Host 'Cambios subidos. GitHub compilará la app (sin crear versión nueva).' }
else { Write-Host "Versión $nueva enviada. En unos 8 minutos estará la Release con el APK:" }
Write-Host "  $web"
Start-Sleep -Seconds 5
gh run watch -R "$Org/$Name" --exit-status 2>$null
if ($LASTEXITCODE -eq 0) { Write-Host "`nCompilación terminada correctamente." -ForegroundColor Green; Start-Process $web }
else { Write-Host "`nSigue el progreso en https://github.com/$Org/$Name/actions" -ForegroundColor Yellow }
