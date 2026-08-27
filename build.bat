@echo off
REM =============================================================================
REM HYDRA-UMC-WATCH - Build and Compile Script
REM Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
REM GPL-3.0 - see LICENSE
REM =============================================================================
REM Compiles the debug APK via the Gradle wrapper. This script is the single
REM source of the real version bump: it increments version.properties AND
REM hydra-umc.project.json together via bump_manifest_version.py, plus
REM versionCode via bump_version_code.py, THEN runs Gradle with
REM -PhydraUmcReadOnly=true so app/build.gradle.kts's own version-bump logic
REM stays inert for this build - that logic still exists and still runs for
REM tools/build_test.py's compile-only CI check (which intentionally must
REM not touch version.properties/the manifest/CHANGELOG.md), but a real
REM build must never bump the version from two places at once.
cd /d "%~dp0"

echo ========================================
echo  HYDRA-UMC-WATCH
echo  Build and Compile Script - compiles the Wear OS debug APK
echo  Author: JuanenRac (Electro Hobby 3D)
echo  E-mail: electrohobby3d@gmail.com
echo  License: GPL-3.0 - see LICENSE
echo ========================================
echo.

echo -- Bumping the version (major.minor.patch + versionCode) --
python "%~dp0bump_manifest_version.py"
if errorlevel 1 ( echo VERSION BUMP FAILED. & pause & exit /b 1 )
python "%~dp0bump_version_code.py"
if errorlevel 1 ( echo VERSION BUMP FAILED. & pause & exit /b 1 )

echo.
echo -- Running the real unit test suite and compiling the debug APK --
set HYDRA_UMC_CI=1
call gradlew.bat testDebugUnitTest assembleDebug -PhydraUmcReadOnly=true
if errorlevel 1 goto :error

echo.
echo ========================================
echo  Build complete. APK: app\build\outputs\apk\debug\app-debug.apk
echo  Run run.bat to install it on a connected Wear OS device/emulator.
echo ========================================
pause
exit /b 0

:error
echo.
echo BUILD FAILED - see the output above.
pause
exit /b 1
