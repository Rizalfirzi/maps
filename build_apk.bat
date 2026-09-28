@echo off
echo ===================================================
echo     MEMBANGUN APK (DEBUG) MMPK ROUTE INSPECTOR
echo ===================================================

call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUKSES] APK berhasil dibuat di:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
) else (
    echo.
    echo [GAGAL] Build gagal. Pastikan Android SDK sudah terpasang dan path di 'local.properties' sudah benar.
    echo.
)
pause
