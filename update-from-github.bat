@echo off
REM HYDRA_UMC_SCRIPT_STANDARD_HEADER_BEGIN
REM *****************************************************************************
REM Project   : HYDRA-UMC-WATCH
REM Script    : update-from-github.bat
REM Purpose   : Download and install a newer signed Wear OS APK through ADB.
REM Author    : JuanenRac (Electro Hobby 3D)
REM Email     : electrohobby3d@gmail.com
REM Copyright : (C) 2026 JuanenRac
REM License   : GPL-3.0 - see LICENSE
REM *****************************************************************************
REM HYDRA_UMC_SCRIPT_STANDARD_HEADER_END
setlocal EnableExtensions DisableDelayedExpansion

echo.
echo *****************************************************************************
echo * HYDRA-UMC-WATCH - update-from-github.bat
echo * Mode      : GITHUB RELEASE ^> ADB WEAR OS UPDATE
echo * Author    : JuanenRac (Electro Hobby 3D)
echo * Email     : electrohobby3d@gmail.com
echo * Copyright : (C) 2026 JuanenRac
echo * License   : GPL-3.0 - see LICENSE
echo * ------------------------------------------------------------------------- *
echo * 1. Read the latest stable GitHub Release metadata over HTTPS.
echo * 2. Compare it with the selected Wear OS device through ADB.
echo * 3. Download the exact signed APK and ask before Android installs it.
echo *****************************************************************************
echo.

REM This updater never changes project files, versions or CHANGELOG.md. It
REM consumes the published release artifact only.
set "HYDRA_UMC_REPOSITORY=JuanenRac/HYDRA-UMC-WATCH"
set "HYDRA_UMC_PACKAGE=com.hydraumc.watch"
set "HYDRA_UMC_ASSET=HYDRA-UMC-WATCH-release.apk"
set "HYDRA_UMC_TEMP_APK=%TEMP%\HYDRA-UMC-WATCH-update.apk"

REM [1/5] Check the required local tools before making any network request.
where powershell >nul 2>nul || (echo [X] PowerShell was not found. & goto :error)
where python >nul 2>nul || (echo [X] Python was not found. It is required for safe version comparison. & goto :error)
set "HYDRA_UMC_ADB=adb"
where adb >nul 2>nul || (
    REM Android Studio's normal Windows SDK location and both standard SDK
    REM environment variables work even when platform-tools is not in PATH.
    if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" set "HYDRA_UMC_ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
    if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "HYDRA_UMC_ADB=%ANDROID_HOME%\platform-tools\adb.exe"
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "HYDRA_UMC_ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)
"%HYDRA_UMC_ADB%" version >nul 2>nul || (echo [X] adb was not found. Install Android Platform Tools first. & goto :error)

REM [2/5] Select exactly one authorized Wear OS device. ADB_SERIAL may be set
REM explicitly when more than one phone, emulator, or watch is connected.
if defined ADB_SERIAL (
    set "HYDRA_UMC_SERIAL=%ADB_SERIAL%"
    "%HYDRA_UMC_ADB%" -s "%HYDRA_UMC_SERIAL%" get-state | findstr /x "device" >nul || (echo [X] ADB_SERIAL is not an authorized online device. & goto :error)
) else (
    set "HYDRA_UMC_DEVICE_COUNT=0"
    for /f "skip=1 tokens=1,2" %%A in ('"%HYDRA_UMC_ADB%" devices') do if "%%B"=="device" (
        set /a HYDRA_UMC_DEVICE_COUNT+=1
        set "HYDRA_UMC_SERIAL=%%A"
    )
    if not "%HYDRA_UMC_DEVICE_COUNT%"=="1" (
        echo [X] Connect and authorize exactly one Wear OS device, or set ADB_SERIAL.
        "%HYDRA_UMC_ADB%" devices
        goto :error
    )
)
echo [OK] Wear OS ADB target: %HYDRA_UMC_SERIAL%

REM [3/5] Ask GitHub only for the latest non-draft, non-prerelease release and
REM extract the deliberately fixed asset name. It refuses an arbitrary APK.
for /f "usebackq tokens=1,2 delims=|" %%A in (`powershell -NoProfile -Command "$r = Invoke-RestMethod -Headers @{Accept='application/vnd.github+json'; 'User-Agent'='HYDRA-UMC-WATCH-updater'} -Uri 'https://api.github.com/repos/%HYDRA_UMC_REPOSITORY%/releases/latest'; if ($r.draft -or $r.prerelease) { exit 2 }; $a = @($r.assets ^| Where-Object { $_.name -eq '%HYDRA_UMC_ASSET%' })[0]; if ($null -eq $a -or -not $a.browser_download_url.StartsWith('https://')) { exit 3 }; Write-Output ($r.tag_name + '|' + $a.browser_download_url)"`) do (
    set "HYDRA_UMC_REMOTE_TAG=%%A"
    set "HYDRA_UMC_DOWNLOAD_URL=%%B"
)
if not defined HYDRA_UMC_REMOTE_TAG (
    echo [X] No valid stable GitHub Release with %HYDRA_UMC_ASSET% was found.
    goto :error
)

for /f "tokens=2 delims==" %%A in ('"%HYDRA_UMC_ADB%" -s "%HYDRA_UMC_SERIAL%" shell dumpsys package %HYDRA_UMC_PACKAGE% ^| findstr /r /c:"^versionName="') do set "HYDRA_UMC_INSTALLED_VERSION=%%A"
if not defined HYDRA_UMC_INSTALLED_VERSION (
    echo [X] %HYDRA_UMC_PACKAGE% is not installed on the selected device.
    echo     Install an initial trusted APK with run.bat or adb install first.
    goto :error
)

REM Semantic comparison rejects malformed tags; it never guesses from a label.
python -c "import re,sys; p=lambda v: tuple(map(int,re.fullmatch(r'v?(\d+)\.(\d+)\.(\d+)',v.strip()).groups())) if re.fullmatch(r'v?(\d+)\.(\d+)\.(\d+)',v.strip()) else None; old,new=p(sys.argv[1]),p(sys.argv[2]); sys.exit(0 if old and new and new>old else 1)" "%HYDRA_UMC_INSTALLED_VERSION%" "%HYDRA_UMC_REMOTE_TAG%"
if errorlevel 1 (
    echo [OK] Watch is already current or the release tag is invalid.
    echo      Installed: v%HYDRA_UMC_INSTALLED_VERSION%  Latest: %HYDRA_UMC_REMOTE_TAG%
    goto :success
)

echo.
echo [4/5] Update available: v%HYDRA_UMC_INSTALLED_VERSION% ^> %HYDRA_UMC_REMOTE_TAG%
set /p "HYDRA_UMC_CONFIRM=Download and install this signed APK on the watch? [y/N]: "
if /I not "%HYDRA_UMC_CONFIRM%"=="Y" (
    echo [INFO] Update cancelled by operator. No APK was downloaded.
    goto :success
)

REM [5/5] GitHub HTTPS downloads to a fixed temporary file, then ADB delegates
REM signature and package validation to Android's installer on the watch.
del /q "%HYDRA_UMC_TEMP_APK%" >nul 2>nul
powershell -NoProfile -Command "Invoke-WebRequest -Uri '%HYDRA_UMC_DOWNLOAD_URL%' -OutFile '%HYDRA_UMC_TEMP_APK%'"
if errorlevel 1 (echo [X] GitHub APK download failed. & goto :error)
"%HYDRA_UMC_ADB%" -s "%HYDRA_UMC_SERIAL%" install -r "%HYDRA_UMC_TEMP_APK%"
if errorlevel 1 (echo [X] Android rejected or could not install the update. & goto :error)
del /q "%HYDRA_UMC_TEMP_APK%" >nul 2>nul
"%HYDRA_UMC_ADB%" -s "%HYDRA_UMC_SERIAL%" shell am start -n %HYDRA_UMC_PACKAGE%/.MainActivity >nul 2>nul
echo [SUCCESS] HYDRA-UMC-WATCH updated to %HYDRA_UMC_REMOTE_TAG%.
goto :success

:error
if exist "%HYDRA_UMC_TEMP_APK%" del /q "%HYDRA_UMC_TEMP_APK%" >nul 2>nul
echo.
echo [FAILED] No project files or versions were changed.
pause
exit /b 1

:success
echo.
echo [INFO] Script completed without changing project files or versions.
pause
exit /b 0
