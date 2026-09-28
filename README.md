# MMPK Offline Route Inspector (Kotlin Android)

Aplikasi Android berbasis **Kotlin** untuk:
1. **Memuat & Mengecek File MMPK Offline** (`.mmpk` - Mobile Map Package) langsung dari penyimpanan internal HP.
2. **Inspeksi Dataset Jaringan Jalan**: Membaca metadata peta, layer spasial, dan mengekstrak `TransportationNetworkDataset` (dataset rute jalan offline).
3. **Pencarian Rute Otomatis (Shortest / Fastest Path)**: Menggunakan algoritma pencarian rute terdekat (Dijkstra / A* via ArcGIS Route Engine) secara 100% offline tanpa koneksi internet.
4. **Petunjuk Arah Navigasi (Turn-by-turn Directions)**: Menampilkan jarak total, estimasi waktu tempuh, dan daftar langkah navigasi belokan demi belokan layaknya Google Maps.

---

## 📱 Arsitektur & Teknologi

- **Bahasa**: Kotlin (Modern Android SDK)
- **Engine Peta & Routing**: ArcGIS Maps SDK for Kotlin (`200.6.0`)
- **UI Design**: Modern Tactical / GIS Dark Mode dengan Material Design 3, ViewBinding, dan RecyclerView
- **Kompatibilitas**: Android 9.0 (API level 28) hingga Android 14+ (API level 34)

---

## 🗂️ Struktur File Project

```text
test_map/
├── app/
│   ├── build.gradle.kts          # Konfigurasi dependensi ArcGIS Maps SDK 200.6.0
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # Permission Storage, Location, OpenGL ES 3.0
│   │   ├── java/com/testmap/mmpk/
│   │   │   ├── MainActivity.kt            # Controller utama: Load MMPK, RouteTask, Tap Map
│   │   │   ├── RouteDirectionAdapter.kt   # Tampilan instruksi navigasi per belokan
│   │   │   ├── MmpkInspectorAdapter.kt    # Tampilan inspeksi dataset & layer jalan
│   │   │   └── model/RoutingModels.kt     # Data model rute dan layer
│   │   └── res/
│   │       ├── layout/activity_main.xml   # MapView + Floating Controls + Bottom Sheet
│   │       ├── layout/dialog_mmpk_info.xml# Dialog info metadata & network dataset
│   │       └── values/                    # Colors, Strings, Themes
├── build_apk.bat                 # Script 1-klik untuk build APK Debug
├── install_to_phone.bat          # Script 1-klik untuk install APK ke HP via USB Debugging
├── local.properties              # Konfigurasi path Android SDK lokal
└── settings.gradle.kts           # Repositori Maven Esri ArcGIS & Google
```

---

## 🚀 Cara Menjalankan & Debug ke HP

### Langkah 1: Hubungkan HP Android ke PC / Laptop
1. Masuk ke **Pengaturan (Settings)** di HP > **Tentang Ponsel (About Phone)**.
2. Ketuk **Nomor Bentukan (Build Number)** sebanyak 7 kali hingga muncul notifikasi *"Anda adalah seorang pengembang!"*.
3. Buka **Opsi Pengembang (Developer Options)** > Aktifkan **USB Debugging**.
4. Hubungkan HP ke PC menggunakan kabel data USB. Jika muncul pop-up izin USB Debugging di layar HP, pilih **Izinkan (Allow)**.

### Langkah 2: Siapkan File `.mmpk` di HP
Salin file `.mmpk` Anda ke dalam penyimpanan HP, misalnya di folder:
- `/sdcard/Download/` (atau folder `Download` di memori internal HP).

### Langkah 3: Build & Install ke HP
Anda dapat memilih salah satu cara berikut:

**Opsi A (Melalui Android Studio):**
1. Buka Android Studio.
2. Pilih **Open**, lalu arahkan ke folder: `E:\BMS apk\test_map`.
3. Tunggu proses Gradle Sync selesai.
4. Pilih perangkat HP Anda di target device dropdown di toolbar atas.
5. Klik tombol **Run (Segitiga Hijau)** atau tekan `Shift + F10`.

**Opsi B (Melalui Command Line / Batch Script):**
1. Pastikan path Android SDK di file [local.properties](file:///e:/BMS%20apk/test_map/local.properties) sudah sesuai dengan lokasi SDK di PC Anda.
2. Klik dua kali file [install_to_phone.bat](file:///e:/BMS%20apk/test_map/install_to_phone.bat).
3. Script akan otomatis membangun APK dan memasangnya ke HP Anda yang terhubung.

---

## 🗺️ Cara Menggunakan Aplikasi di HP

1. **Memuat File MMPK**:
   - Tekan tombol **"Pilih File MMPK"** untuk membuka pengelola file bawaan HP dan memilih file `.mmpk`.
   - Atau tekan **"Path /sdcard"** untuk memasukkan path manual file jika Anda tahu lokasinya (misal `/sdcard/Download/peta_offline.mmpk`).
2. **Cek Dataset Jalan (Transportation Network Dataset)**:
   - Tekan tombol **"Dataset"** di pojok kanan atas.
   - Anda akan melihat daftar layer dan status dataset rute jalan:
     - 🛣️ **Transportation Network Dataset (Routing Active)**: Menandakan MMPK memiliki dataset jalan terkompilasi untuk routing offline.
     - 🗺️ **Layer Map**: Layer spasial visual (jalan, bangunan, poligon).
3. **Membuat Rute Terdekat**:
   - Tekan tombol **A (Awal)**, lalu ketuk posisi awal di peta (akan muncul pin hijau).
   - Tekan tombol **B (Tujuan)**, lalu ketuk posisi tujuan di peta (akan muncul pin merah).
   - Tekan tombol **"Cari Rute"**:
     - Aplikasi akan mengeksekusi `RouteTask` secara offline.
     - Jalur rute jalan terdekat (garis cyan bercahaya) akan digambar di atas jaringan jalan.
     - Jarak total (km) dan perkiraan waktu tempuh akan muncul di panel bawah.
   - Tekan **"Petunjuk Arah"** untuk melihat panduan belokan demi belokan (Turn-by-turn navigation) seperti Google Maps.
