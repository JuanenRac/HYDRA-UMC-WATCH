@echo off
REM HYDRA_UMC_SCRIPT_STANDARD_HEADER_BEGIN
REM *****************************************************************************
REM Project   : HYDRA-UMC-WATCH
REM Script    : run.bat
REM Purpose   : Runtime workflow for the project entry point.
REM Author    : JuanenRac (Electro Hobby 3D)
REM Email     : electrohobby3d@gmail.com
REM Copyright : (C) 2026 JuanenRac
REM License   : GPL-3.0 - see LICENSE
REM *****************************************************************************
REM HYDRA_UMC_SCRIPT_STANDARD_HEADER_END
REM HYDRA_UMC_SCRIPT_STANDARD_BANNER_BEGIN
echo.
echo *****************************************************************************
echo * HYDRA-UMC-WATCH - run.bat
echo * Mode      : RUN WORKFLOW
echo * Author    : JuanenRac (Electro Hobby 3D)
echo * Email     : electrohobby3d@gmail.com
echo * Copyright : (C) 2026 JuanenRac
echo * License   : GPL-3.0 - see LICENSE
echo * ------------------------------------------------------------------------- *
echo * 1. Resolve the runtime prerequisites declared by this script.
echo * 2. Start the project entry point and forward user arguments unchanged.
echo * 3. Preserve its result and keep an interactive terminal open.
echo *****************************************************************************
echo.
REM HYDRA_UMC_SCRIPT_STANDARD_BANNER_END
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

REM HYDRA_UMC_SCRIPT_STANDARD_SAFE_PAUSE
set "HYDRA_UMC_SCRIPT_RESULT=%ERRORLEVEL%"
echo.
echo [INFO] Script completed. Exit code: %HYDRA_UMC_SCRIPT_RESULT%.
pause
exit /b %HYDRA_UMC_SCRIPT_RESULT%
