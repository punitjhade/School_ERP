@echo off
REM Compiles and runs VSVPS Connect. Usage: run.bat [port]
setlocal enabledelayedexpansion
cd /d "%~dp0"
set PORT=%1
if "%PORT%"=="" set PORT=8080

echo Compiling...
if not exist bin mkdir bin
if exist sources.txt del sources.txt
for /f "delims=" %%f in ('dir /s /b src\*.java') do (
    set "p=%%f"
    set "p=!p:\=/!"
    echo "!p!">>sources.txt
)
javac -d bin @sources.txt
if errorlevel 1 (
    echo.
    echo Compilation failed - see errors above.
    del sources.txt
    exit /b 1
)
del sources.txt

echo Starting server on port %PORT% ...
java -cp bin vsvps.Main %PORT%
