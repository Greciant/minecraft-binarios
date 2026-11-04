@echo off
chcp 65001 >nul
title Ports a Forge 1.20.1 - 4-oct
echo ================================================================
echo   PORTS PROPIOS A FORGE 1.20.1 - 4-oct-2026
echo ================================================================
echo.
echo   + Ancient Remnants: Monoliths  monolitos y bendiciones
echo   + Timeless Echoes              dimension roguelite de sculk
echo   + Noble Phantasms              reliquias legendarias y enemigos
echo   + Archaion                     fortaleza antigua y su jefe
echo   + AAA Particles 2.2.3          lo necesita Archaion
echo.
echo   El servidor ya los tiene. Uso personal: no compartir estos jars.
echo.
echo   CIERRA MINECRAFT ANTES DE SEGUIR.
echo.
pause
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aplicar-ports.ps1"
echo.
pause
