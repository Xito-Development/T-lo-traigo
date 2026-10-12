@echo off
rem Ejecutar UNA SOLA VEZ: crea la clave de firma y la guarda en GitHub
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\configurar-firma.ps1" %*
pause
