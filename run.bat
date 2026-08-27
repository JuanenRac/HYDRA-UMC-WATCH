@echo off
REM =============================================================================
REM HYDRA-UMC-WATCH - Run Script
REM Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
REM GPL-3.0 - see LICENSE
REM =============================================================================
REM Installs the debug APK onto a connected Wear OS device/emulator (via
REM Gradle's installDebug task, which calls adb install internally) and
REM launches MainActivity. Run build.bat first, or just run this - Gradle
REM builds installDebug's own dependencies (assembleDebug) automatically.
cd /d "%~dp0"

call gradlew.bat installDebug
if errorlevel 1 goto :error

adb shell am start -n com.hydraumc.watch/.MainActivity
exit /b 0

:error
echo.
echo INSTALL FAILED - is a Wear OS device/emulator connected? Check "adb devices".
exit /b 1
