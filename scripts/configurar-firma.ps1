<#
  Configura UNA SOLA VEZ la firma de la app y los secretos de GitHub (Windows).
  Uso: .\scripts\configurar-firma.ps1 [-PlayJson C:\ruta\cuenta-servicio.json]
  Guarda la clave en %USERPROFILE%\TLoTraigo-firma\ (¡haz copia de seguridad! sin ella no podrás actualizar la app).
#>
param([string]$PlayJson = '')
$ErrorActionPreference = 'Continue'
$Repo = 'Xito-Development/T-lo-traigo'
$Dir = Join-Path $env:USERPROFILE 'TLoTraigo-firma'
$Jks = Join-Path $Dir 'release.jks'; $PassFile = Join-Path $Dir 'contraseña.txt'
New-Item -ItemType Directory -Force $Dir | Out-Null

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
  winget install --id GitHub.cli -e --silent --accept-source-agreements --accept-package-agreements | Out-Null
  $env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')
}
gh auth status *> $null; if ($LASTEXITCODE -ne 0) { gh auth login --web --git-protocol https }

# Buscar keytool (Android Studio, JAVA_HOME o instalar Temurin 17)
$kt = @("$env:ProgramFiles\Android\Android Studio\jbr\bin\keytool.exe", "$env:JAVA_HOME\bin\keytool.exe") + `
      (Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium\*\bin\keytool.exe" -ErrorAction SilentlyContinue | ForEach-Object FullName) |
      Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $kt) {
  winget install --id EclipseAdoptium.Temurin.17.JDK -e --silent --accept-source-agreements --accept-package-agreements | Out-Null
  $kt = Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium\*\bin\keytool.exe" | Select-Object -First 1 -ExpandProperty FullName
}

if (Test-Path $Jks) {
  Write-Host "Ya existe $Jks, se reutiliza."
  $pass = (Get-Content $PassFile -Raw).Trim()
} else {
  $pass = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 28 | ForEach-Object { [char]$_ })
  & $kt -genkeypair -keystore $Jks -alias tlotraigo -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $pass -keypass $pass -dname "CN=Xito Development, O=Xito Development, L=Leon, C=ES"
  [IO.File]::WriteAllText($PassFile, $pass)
}

Write-Host 'Guardando secretos en GitHub...'
[Convert]::ToBase64String([IO.File]::ReadAllBytes($Jks)) | gh secret set KEYSTORE_BASE64 -R $Repo
$pass | gh secret set KEYSTORE_PASSWORD -R $Repo
$pass | gh secret set KEY_PASSWORD -R $Repo
'tlotraigo' | gh secret set KEY_ALIAS -R $Repo
if ($PlayJson) { Get-Content $PlayJson -Raw | gh secret set PLAY_SERVICE_ACCOUNT_JSON -R $Repo; Write-Host 'Secreto de Google Play guardado.' }

Write-Host "`nListo. Clave guardada en $Dir" -ForegroundColor Green
Write-Host 'IMPORTANTE: copia esa carpeta a un lugar seguro (USB, gestor de contraseñas). Si la pierdes no podrás actualizar la app.' -ForegroundColor Yellow
