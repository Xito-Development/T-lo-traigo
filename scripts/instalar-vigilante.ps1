<# Instala (o desinstala con -Desinstalar) el vigilante de Descargas para que arranque al iniciar sesión, oculto. #>
param([switch]$Desinstalar)
$Nombre = 'TLoTraigo - Vigilante de Descargas'
$Script = Join-Path $PSScriptRoot 'vigilar-descargas.ps1'
Get-ScheduledTask -TaskName $Nombre -ErrorAction SilentlyContinue | ForEach-Object {
  Stop-ScheduledTask -TaskName $Nombre -ErrorAction SilentlyContinue
  Unregister-ScheduledTask -TaskName $Nombre -Confirm:$false
}
if ($Desinstalar) { Write-Host 'Vigilante desinstalado.' -ForegroundColor Yellow; return }
$accion = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$Script`""
$inicio = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$ajustes = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) `
  -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew
Register-ScheduledTask -TaskName $Nombre -Action $accion -Trigger $inicio -Settings $ajustes -Description 'Aplica al proyecto los ZIP de T lo traigo descargados' | Out-Null
Start-ScheduledTask -TaskName $Nombre
Write-Host "Listo. Desde ahora, al descargar tlotraigo*.zip, web-t-lo-traigo*.zip o CLAUDE.md se aplican solos en:" -ForegroundColor Green
Write-Host "  $(Split-Path $PSScriptRoot -Parent)"
Write-Host 'Registro: .descargas\registro.txt  ·  Para quitarlo: instalar-vigilante.bat -Desinstalar'
