# X-Mod Launcher

Launcher Android bergaya terminal premium klasik (amber). Versi awal (0.1).

## Fitur versi ini
- Home terminal: jam besar, tanggal, baris sistem (baterai), prompt `x-mod@s24:~`.
- Laci aplikasi: menampilkan **semua aplikasi terpasang** dalam grid.
- **Ikon otomatis di-reskin** jadi sketsa garis amber — berlaku ke semua aplikasi
  (ikon aslinya diproses saat ditampilkan, aplikasi lain tidak diubah).
- Pencarian aplikasi ala terminal (`grep>`).
- Bisa dijadikan launcher default (`> set default` di pojok header, atau tekan Home).

## Cara dapat APK
Dibangun otomatis oleh GitHub Actions. Setelah build hijau, unduh di:
**Releases → X-Mod Launcher → X-Mod-Launcher.apk**, atau artifact **xmod-apk** di tab Actions.

## Pasang & aktifkan
1. Pasang `X-Mod-Launcher.apk` (izinkan sumber tak dikenal).
2. Tekan tombol **Home** → pilih **X-Mod Launcher** → **Selalu**.
   Atau buka X-Mod lalu ketuk `> set default`.
3. Untuk kembali ke launcher Samsung: Setelan → Aplikasi → Aplikasi default → Aplikasi layar utama.

## Membangun sendiri
Butuh Android SDK + JDK 17.

```
cd xmod
gradle assembleDebug
```
