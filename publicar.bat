@echo off
rem Doble clic para publicar una nueva versión de T lo traigo
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\publicar.ps1" %*
pause
