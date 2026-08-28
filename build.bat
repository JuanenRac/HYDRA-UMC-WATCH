@echo off
REM HYDRA_UMC_SCRIPT_STANDARD_HEADER_BEGIN
REM *****************************************************************************
REM Project   : HYDRA-UMC-WATCH
REM Script    : build.bat
REM Purpose   : Incremental project build, verification and packaging workflow.
REM Author    : JuanenRac (Electro Hobby 3D)
REM Email     : electrohobby3d@gmail.com
REM Copyright : (C) 2026 JuanenRac
REM License   : GPL-3.0 - see LICENSE
REM *****************************************************************************
REM HYDRA_UMC_SCRIPT_STANDARD_HEADER_END
REM HYDRA_UMC_SCRIPT_STANDARD_BANNER_BEGIN
echo.
echo *****************************************************************************
echo * HYDRA-UMC-WATCH - build.bat
echo * Mode      : INCREMENTAL BUILD
echo * Author    : JuanenRac (Electro Hobby 3D)
echo * Email     : electrohobby3d@gmail.com
echo * Copyright : (C) 2026 JuanenRac
echo * License   : GPL-3.0 - see LICENSE
echo * ------------------------------------------------------------------------- *
echo * 1. Increment the project version and synchronise its manifest.
echo * 2. Run this project's declared build, verification and packaging commands.
echo * 3. Report the result and keep an interactive terminal open.
echo *****************************************************************************
echo.
REM HYDRA_UMC_SCRIPT_STANDARD_BANNER_END
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
REM HYDRA_UMC_SCRIPT_STANDARD_VERSION_STEP
echo [1/3] Incrementing project version and synchronising its manifest...
REM HYDRA_UMC_SCRIPT_STANDARD_VERSION_CAPTURE_BEFORE
for /f "usebackq delims=" %%V in (`python -c "import json; print(json.load(open(r'%~dp0hydra-umc.project.json', encoding='utf-8'))['version'])"`) do set "HYDRA_UMC_VERSION_BEFORE=%%V"
python "%~dp0bump_manifest_version.py"
if errorlevel 1 ( echo VERSION BUMP FAILED. & pause & exit /b 1 )
REM HYDRA_UMC_SCRIPT_STANDARD_VERSION_CAPTURE_AFTER
for /f "usebackq delims=" %%V in (`python -c "import json; print(json.load(open(r'%~dp0hydra-umc.project.json', encoding='utf-8'))['version'])"`) do set "HYDRA_UMC_VERSION_AFTER=%%V"
if not defined HYDRA_UMC_VERSION_BEFORE set "HYDRA_UMC_VERSION_BEFORE=unknown"
if not defined HYDRA_UMC_VERSION_AFTER set "HYDRA_UMC_VERSION_AFTER=unknown"
echo.
echo *****************************************************************************
echo * VERSION INCREMENT COMPLETED
echo * v%HYDRA_UMC_VERSION_BEFORE% ^> v%HYDRA_UMC_VERSION_AFTER%
echo * Project manifest has been synchronised by the project build flow.
echo *****************************************************************************
echo.
echo.
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
