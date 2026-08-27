@echo off
REM =============================================================================
REM HYDRA-UMC-WATCH - Build and Compile Script
REM Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
REM GPL-3.0 - see LICENSE
REM =============================================================================
python "%~dp0bump_manifest_version.py"
if errorlevel 1 ( echo VERSION BUMP FAILED. & pause & exit /b 1 )
REM Compiles the debug APK via the Gradle wrapper. Bumping the version
REM happens inside app/build.gradle.kts itself (Gradle configuration time),
REM so it runs automatically as part of this same command - no separate
REM version-bump step needed, unlike the Node/Go/Python projects.
cd /d "%~dp0"

echo ========================================
echo  HYDRA-UMC-WATCH
echo  Build and Compile Script - compiles the Wear OS debug APK
echo  Author: JuanenRac (Electro Hobby 3D)
echo  E-mail: electrohobby3d@gmail.com
echo  License: GPL-3.0 - see LICENSE
echo ========================================
echo.

call gradlew.bat assembleDebug
if errorlevel 1 goto :error

echo.
echo ========================================
echo  Build complete. APK: app\build\outputs\apk\debug\app-debug.apk
echo  Run run.bat to install it on a connected Wear OS device/emulator.
echo ========================================
exit /b 0

:error
echo.
echo BUILD FAILED - see the output above.
exit /b 1
