@echo off
REM Maven Wrapper Script for Loan Origination System (Windows)
REM This script manages all microservices

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PID_FILE=%SCRIPT_DIR%.services.pid"
set "LOG_DIR=%SCRIPT_DIR%logs"

REM Service modules (excluding common)
set "SERVICES=api-gateway:8080 orchestrator:8081 decision-engine:8082 experian-connector:8083 equifax-connector:8084 audit-logging:8085"

REM Create logs directory
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

if "%1"=="start" goto start_services
if "%1"=="stop" goto stop_services
if "%1"=="restart" goto restart_services
if "%1"=="status" goto status_services
if "%1"=="build" goto build_all
goto usage

:start_services
echo Starting Loan Origination System Services...
echo.

REM Check if services are already running
if exist "%PID_FILE%" (
    echo Services appear to be already running. Use 'stop' command first.
    exit /b 1
)

REM Create/clear PID file
type nul > "%PID_FILE%"

REM Start each service
for %%s in (%SERVICES%) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        set "module=%%a"
        set "port=%%b"
        echo Starting !module!...
        
        cd /d "%SCRIPT_DIR%"
        start "!module!" /min cmd /c "mvn spring-boot:run -pl !module! > %LOG_DIR%\!module!.log 2>&1"
        
        REM Store process info (simplified for Windows)
        echo !module! >> "%PID_FILE%"
        
        timeout /t 3 /nobreak > nul
    )
)

echo.
echo All services started!
echo Check logs in: %LOG_DIR%
echo.
echo To stop all services: run-services.bat stop
echo.
goto end

:stop_services
echo Stopping all services...
echo.

if not exist "%PID_FILE%" (
    echo No PID file found. Services may not be running.
    goto end
)

REM Kill all Java processes related to Spring Boot
taskkill /FI "WINDOWTITLE eq api-gateway*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq orchestrator*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq decision-engine*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq experian-connector*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq equifax-connector*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq audit-logging*" /T /F >nul 2>&1

REM Also try to kill by port
for /l %%p in (8080,1,8085) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%p" ^| findstr "LISTENING"') do (
        taskkill /PID %%a /F >nul 2>&1
    )
)

del "%PID_FILE%" >nul 2>&1
echo All services stopped.
goto end

:restart_services
call :stop_services
timeout /t 2 /nobreak > nul
call :start_services
goto end

:status_services
echo Service Status:
echo.

if not exist "%PID_FILE%" (
    echo No services appear to be running.
    goto end
)

echo Checking service status...
for %%s in (%SERVICES%) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        set "module=%%a"
        set "port=%%b"
        netstat -an | findstr ":%%b" >nul
        if !errorlevel! equ 0 (
            echo [RUNNING] %%a on port %%b
        ) else (
            echo [STOPPED] %%a on port %%b
        )
    )
)
goto end

:build_all
echo Building all services...
cd /d "%SCRIPT_DIR%"
call mvn clean install -DskipTests
goto end

:usage
echo Usage: %0 {start^|stop^|restart^|status^|build}
echo.
echo Commands:
echo   start    - Start all microservices
echo   stop     - Stop all microservices
echo   restart  - Restart all microservices
echo   status   - Show status of all services
echo   build    - Build all services
exit /b 1

:end
endlocal


