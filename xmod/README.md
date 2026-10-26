# X-Mod Launcher

Launcher Android bergaya terminal premium klasik (amber). Versi 0.2.

## Tampilan (Home v2)
- **Status bar X-Mod sendiri**: jam, tipe jaringan, baterai (data asli HP).
- **Header `status --live`**: baris ala Kali Linux, jam besar + detik, tanggal, minggu.
- **Panel `[ sys ]`**: CPU (inti×GHz), RAM, penyimpanan, jaringan, baterai, uptime — semua data asli.
- **Panel `[ net/s ]`**: grafik lalu lintas jaringan yang bergerak.
- **Panel `[ log ]`**: log sistem + kursor berkedip.
- **Foto pilihanmu** sebagai wallpaper di bagian bawah (fade ke hitam, mode amber/asli). Foto tetap di HP, tidak diunggah ke mana pun.
- **Dock**: WhatsApp, Chrome, Claude, Gaming Hub (ikon sketsa amber).

## Cara pakai (sengaja tanpa petunjuk, biar bersih & rumit)
- **Buka laci aplikasi**: tekan lama baris `x-mod㉿s24` (atau ketik `apps` di prompt).
- **Prompt rahasia** `x-mod:~$` — ketik perintah, mis. `wa`, `chrome`, `claude`, `game`,
  `cam`, `opt`, `foto`, `setelan`, atau nama aplikasi apa pun. Perintah tak dikenal → `command not found`.
- **Ganti foto**: ketik `foto`.
- **OPTIMIZE**: animasi scan terminal, membersihkan cache X-Mod sendiri, lalu
  membuka **Device Care Samsung** untuk pembersihan dalam.

## Update (tanpa uninstall)
Semua build ditandatangani dengan **kunci tetap**, jadi versi baru langsung
menimpa versi lama. Cara termudah update otomatis:

1. Pasang **Obtainium** (`ImranR98/Obtainium`).
2. Add App → tempel: `https://github.com/Tendiriana/cerita-jadi-simbol`
3. Obtainium memberi tahu tiap ada versi X-Mod baru di Releases.

## Jadikan launcher default
Tekan **Home** → pilih **X-Mod Launcher** → **Selalu**.
Balik ke Samsung: Setelan → Aplikasi → Aplikasi default → Aplikasi layar utama.

## Catatan izin
Launcher ini hanya meminta izin **membaca status jaringan**. Tidak ada akses
internet, tidak memasang aplikasi, tidak menghentikan paksa aplikasi lain.

## Build sendiri
Butuh Android SDK + JDK 17.
```
cd xmod
gradle assembleDebug
```

Font: JetBrains Mono (SIL OFL 1.1, lihat FONT-LICENSE-OFL.txt).
