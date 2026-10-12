<#
  Instala, reconfigura o desinstala el vigilante de Descargas.
    instalar-vigilante.bat                 instala (pregunta la carpeta del proyecto)
    cambiar-ubicacion.bat                  cambia la carpeta donde se aplican los ZIP
    instalar-vigilante.bat -Desinstalar    lo quita
  Opciones: -Proyecto "C:\ruta" -Descargas "C:\ruta" -SinGit (también reemplaza la carpeta .git)
#>
param([switch]$Desinstalar, [switch]$CambiarUbicacion, [string]$Proyecto = '', [string]$Descargas = '', [switch]$SinGit)
$Nombre = 'TLoTraigo - Vigilante de Descargas'
$App = Join-Path $env:LOCALAPPDATA 'TLoTraigo'
$CfgFile = Join-Path $App 'vigilante.json'
$ScriptFijo = Join-Path $App 'vigilar-descargas.ps1'   # copia fuera del proyecto: así puede reemplazarlo sin borrarse a sí mismo
New-Item -ItemType Directory -Force $App | Out-Null

function Parar {
  Get-ScheduledTask -TaskName $Nombre -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-ScheduledTask -TaskName $Nombre -ErrorAction SilentlyContinue
    Unregister-ScheduledTask -TaskName $Nombre -Confirm:$false
  }
  Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like '*vigilar-descargas.ps1*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
function ElegirCarpeta($titulo, $inicial) {
  Add-Type -AssemblyName System.Windows.Forms
  $d = New-Object System.Windows.Forms.FolderBrowserDialog
  $d.Description = $titulo; $d.ShowNewFolderButton = $true
  if ($inicial -and (Test-Path $inicial)) { $d.SelectedPath = $inicial }
  if ($d.ShowDialog() -eq 'OK') { return $d.SelectedPath } else { return $null }
}

if ($Desinstalar) { Parar; Write-Host 'Vigilante desinstalado.' -ForegroundColor Yellow; return }

$cfg = if (Test-Path $CfgFile) { Get-Content $CfgFile -Raw -Encoding utf8 | ConvertFrom-Json } else { $null }
$actual = if ($cfg) { $cfg.Proyecto } else { Split-Path $PSScriptRoot -Parent }
if (-not $Proyecto) {
  if ($CambiarUbicacion -or -not $cfg) {
    Write-Host 'Elige la carpeta donde se aplicarán los ZIP del proyecto...'
    $Proyecto = ElegirCarpeta 'Carpeta del proyecto T lo traigo (se reemplaza con cada ZIP descargado)' $actual
    if (-not $Proyecto) { Write-Host 'Cancelado.' -ForegroundColor Yellow; return }
  } else { $Proyecto = $actual }
}
if (-not $Descargas) { $Descargas = if ($cfg -and $cfg.Descargas) { $cfg.Descargas } else { (New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path } }
$conservar = if ($SinGit) { $false } elseif ($cfg -and $null -ne $cfg.ConservarGit) { [bool]$cfg.ConservarGit } else { $true }
$copias = if ($cfg -and $cfg.Copias) { [int]$cfg.Copias } else { 5 }
Add-Type -AssemblyName System.Windows.Forms
$r = [System.Windows.Forms.MessageBox]::Show("¿Quieres que cada ZIP del proyecto que descargues se PUBLIQUE solo?`n`nSí: descargas el ZIP y en unos 10 minutos la nueva versión llega a los móviles.`nNo: solo se copia a tu carpeta y publicas tú con publicar.bat.",
  'T lo traigo', 'YesNo', 'Question')
$auto = ($r -eq 'Yes')

$json = [ordered]@{ Proyecto = $Proyecto; Descargas = $Descargas; ConservarGit = $conservar; Copias = $copias; AutoPublicar = $auto } | ConvertTo-Json
[IO.File]::WriteAllText($CfgFile, $json, (New-Object Text.UTF8Encoding $false))
Copy-Item (Join-Path $PSScriptRoot 'vigilar-descargas.ps1') $ScriptFijo -Force

Parar
$accion = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$ScriptFijo`""
$inicio = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$ajustes = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) `
  -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew
Register-ScheduledTask -TaskName $Nombre -Action $accion -Trigger $inicio -Settings $ajustes -Description 'Aplica al proyecto los ZIP de T lo traigo descargados' | Out-Null
Start-ScheduledTask -TaskName $Nombre

Write-Host "`nListo." -ForegroundColor Green
Write-Host "  Proyecto:  $Proyecto   (cada tlotraigo*.zip lo REEMPLAZA)"
Write-Host "  Descargas: $Descargas"
Write-Host "  Publicar solo: $(if ($auto) { 'SÍ' } else { 'no' })"
Write-Host "  .git:      $(if ($conservar) { 'se conserva' } else { 'también se reemplaza' })"
Write-Host "  Copias de seguridad (últimas $copias): $App\copias"
Write-Host "  Registro:  $App\registro.txt"
Write-Host 'Para cambiar la carpeta: cambiar-ubicacion.bat'
