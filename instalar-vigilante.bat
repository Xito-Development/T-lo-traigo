@echo off
rem Instala el vigilante: los ZIP de T lo traigo que descargues se aplican solos al proyecto
powershell -NoProfile -STA -ExecutionPolicy Bypass -File "%~dp0scripts\instalar-vigilante.ps1" %*
pause
