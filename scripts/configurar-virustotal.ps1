# Guarda la clave de VirusTotal como secreto de GitHub (VT_API_KEY). Cada versión publicada se analizará sola.
$ErrorActionPreference = 'Stop'
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { winget install --id GitHub.cli -e --silent; $env:Path += ";$env:ProgramFiles\GitHub CLI" }
gh auth status 2>$null; if ($LASTEXITCODE -ne 0) { gh auth login -w }
$k = Read-Host "Pega tu clave API de VirusTotal (virustotal.com → tu perfil → API key)" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($k))
$plain | gh secret set VT_API_KEY --repo Xito-Development/T-lo-traigo
Write-Host "Listo: las próximas versiones se analizarán en VirusTotal y el enlace saldrá en las notas, en la web y en el README." -ForegroundColor Green
pause
