@echo off
echo ===================================================
echo     INSTALL & DEBUG APK KE HP ANDROID (VIA ADB)
echo ===================================================

set ADB_CMD="E:\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB_CMD% (
    set ADB_CMD=adb
)

echo Menggunakan ADB: %ADB_CMD%
echo.
echo Memeriksa perangkat Android yang terhubung via USB:
%ADB_CMD% devices
echo.

set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo [INFO] File APK belum ada, menjalankan build terlebih dahulu...
    call gradlew.bat assembleDebug
)

if exist "%APK_PATH%" (
    echo Menginstal APK ke perangkat...
    %ADB_CMD% install -r "%APK_PATH%"
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo [SUKSES] Aplikasi berhasil diinstal ke HP!
        echo Menjalankan aplikasi di HP...
        %ADB_CMD% shell am start -n com.testmap.mmpk/.MainActivity
    ) else (
        echo.
        echo [GAGAL] Pastikan HP terhubung dengan USB Debugging aktif di Opsi Pengembang (Developer Options).
    )
) else (
    echo [GAGAL] File APK tidak ditemukan.
)

pause
