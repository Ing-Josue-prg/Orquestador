@echo off
title Orquestador de Evaluacion de Calidad de Software
cd /d "%~dp0"

echo Iniciando el Orquestador de Evaluacion de Calidad de Software...
echo (la primera vez puede tardar unos minutos mientras Maven descarga dependencias)
echo.

call mvn -q -pl app javafx:run

if errorlevel 1 (
    echo.
    echo No se pudo iniciar la aplicacion. Revisa el mensaje de arriba.
    echo Verifica que tengas instalados JDK 21 y Maven, y que esten en el PATH.
    pause
)
