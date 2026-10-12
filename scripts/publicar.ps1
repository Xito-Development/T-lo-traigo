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
  [switch]$SoloSubir   # sube los cambios sin crear versión nueva
)
$ErrorActionPreference = 'Continue'   # git escribe avisos por stderr; los errores se comprueban con LASTEXITCODE
function Escribir($ruta, $texto) { [IO.File]::WriteAllText((Join-Path (Get-Location) $ruta), $texto, (New-Object Text.UTF8Encoding $false)) }  # UTF-8 sin BOM
$Org = 'Xito-Development'; $Name = 'T-lo-traigo'
$RepoUrl = "https://github.com/$Org/$Name.git"
Set-Location (Split-Path $PSScriptRoot -Parent)

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
$props = Get-Content version.properties -Raw -Encoding utf8
$actual = ([regex]'VERSION_NAME=(\S+)').Match($props).Groups[1].Value
$publicada = (git ls-remote --tags origin "refs/tags/v$actual") -ne $null
if ($SoloSubir) { $nueva = $actual }
elseif (-not $publicada) { $nueva = $actual }   # primera publicación de esta versión
else {
  $p = $actual.Split('-')[0].Split('.') | ForEach-Object { [int]$_ }
  switch ($Tipo) { 'mayor' { $p = @($p[0]+1,0,0) } 'menor' { $p = @($p[0],$p[1]+1,0) } default { $p = @($p[0],$p[1],$p[2]+1) } }
  $nueva = ($p -join '.')
}

if (-not $SoloSubir) {
  if (-not $Mensaje) { $Mensaje = Read-Host "¿Qué ha cambiado en la versión $nueva? (una frase)" }
  if (-not $Mensaje) { $Mensaje = 'Mejoras y correcciones.' }
  Escribir version.properties "VERSION_NAME=$nueva"
  $log = Get-Content CHANGELOG.md -Raw -Encoding utf8
  if ($log -notmatch [regex]::Escape("## [$nueva]")) {
    $fecha = Get-Date -Format 'yyyy-MM-dd'
    $entrada = "## [$nueva] - $fecha`n### Cambiado`n- $Mensaje`n`n"
    $i = $log.IndexOf('## [')
    $log = if ($i -ge 0) { $log.Insert($i, $entrada) } else { $log + "`n" + $entrada }
    Escribir CHANGELOG.md $log
  }
  $nota = if ($Mensaje.Length -gt 480) { $Mensaje.Substring(0,480) } else { $Mensaje }
  Escribir fastlane/metadata/android/es-ES/changelogs/default.txt "$nota`n"
}

Paso 'Subiendo a GitHub'
git add -A
$titulo = if ($SoloSubir) { 'Actualización' } else { "Versión ${nueva}: $Mensaje" }
git diff --cached --quiet; if ($LASTEXITCODE -ne 0) { git commit -m $titulo | Out-Null }
git push -u origin main
if ($LASTEXITCODE -ne 0) { throw 'No se pudo subir. Revisa que tienes permisos en la organización.' }

Paso 'Listo'
if ($SoloSubir) { Write-Host 'Cambios subidos. GitHub compilará la app (sin crear versión nueva).' }
else { Write-Host "Versión $nueva enviada. En unos 8 minutos estará la Release con el APK:" }
Write-Host "  https://github.com/$Org/$Name/releases/latest"
Start-Sleep -Seconds 5
gh run watch -R "$Org/$Name" --exit-status 2>$null
if ($LASTEXITCODE -eq 0) { Write-Host "`nCompilación terminada correctamente." -ForegroundColor Green; Start-Process "https://github.com/$Org/$Name/releases/latest" }
else { Write-Host "`nSigue el progreso en https://github.com/$Org/$Name/actions" -ForegroundColor Yellow }
