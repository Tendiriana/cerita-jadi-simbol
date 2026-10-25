# Senter

Aplikasi Android sederhana: satu tombol untuk menyalakan dan mematikan lampu senter (flash kamera).

## Cara mendapatkan file APK

APK dibangun otomatis oleh GitHub Actions setiap kali folder `senter/` berubah.

1. Buka tab **Actions** di repositori ini.
2. Pilih run **Build Senter APK** yang paling atas (tunggu sampai tanda centang hijau).
3. Di bagian bawah halaman, unduh artifact **senter-apk**.
4. Ekstrak file zip-nya, lalu pasang `app-debug.apk` di HP Android.

Saat memasang, izinkan "Instal aplikasi tidak dikenal". Aplikasi ini tidak meminta izin apa pun dan tidak menggunakan internet.

## Membangun sendiri (opsional)

Butuh Android SDK dan JDK 17.

```
cd senter
./gradlew assembleDebug
```

Hasilnya ada di `app/build/outputs/apk/debug/app-debug.apk`.
