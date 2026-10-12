@echo off
rem Cambia la carpeta donde el vigilante aplica los ZIP descargados
powershell -NoProfile -STA -ExecutionPolicy Bypass -File "%~dp0scripts\instalar-vigilante.ps1" -CambiarUbicacion %*
pause
