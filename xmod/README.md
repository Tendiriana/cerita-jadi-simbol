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
- **Buka laci aplikasi**: tekan lama baris `x-mod㉿s24` (atau ketik `apps`).
- **Ganti launcher default**: tekan lama `[ session:active ]` (kanan atas).
- **Prompt rahasia** `x-mod:~$` — ketik perintah. Muncul chip saran; `↺` = riwayat.
- **Perintah**: `help`, `neofetch`, `ps`, `whoami`, `date`, `uname`, `echo`, `clear`,
  `apps`, `opt`, `foto`, `cam`, `setelan`, `default`, atau nama aplikasi.
- **Tema**: `theme amber | green | red`.
- **Boot**: `boot on` / `boot off`.
- **App vault (sembunyikan aplikasi)**: `hide apps` membuka vault; `hide <app>` /
  `unhide <app>`, atau tekan lama ikon di laci (sembunyikan) / di vault (tampilkan).
- **Easter egg**: salah perintah 3× → layar peringatan.
- **OPTIMIZE**: animasi scan, bersihkan cache X-Mod, lalu buka **Device Care Samsung**.

## Panel kontrol (0.4)
Usap ke bawah di home (atau ketik `panel`) → panel amber:
- **Senter** (langsung), **kecerahan** (slider), **rotasi**, **DND** (toggle).
- Ubin **WiFi / Bluetooth / Data / Pesawat / Lokasi / Setelan** membuka panel sistem.
- Rotasi & kecerahan butuh izin "ubah setelan sistem"; DND butuh "akses Jangan Ganggu" — diberikan manual sekali saat pertama dipakai.

Tambahan vibe: boot sequence, efek ketik, getar halus, glitch,
status charging (baterai ⚡ beranimasi).

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
