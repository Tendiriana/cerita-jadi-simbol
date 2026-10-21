# TakTik IPA

Latihan soal cerita IPA SMP. Siswa mengubah kalimat soal menjadi besaran, mengenali materinya, memilih rumus, lalu menghitung. Guru mengelola soal lewat **Pengaturan → Masuk mode guru**.

## Isi soal

81 soal kelas 9 yang mengikuti Buku IPA SMP/MTs Kelas IX (Edisi Revisi), Kurikulum Merdeka:

| Materi | Jumlah | Isi |
|---|---|---|
| Listrik | 21 | Coulomb, medan dan beda potensial, I = q/t, hukum Ohm, seri, paralel, hambatan jenis, daya dan energi |
| Magnet | 20 | Gaya Lorentz F = I·L·B sin θ, GGL induksi ε = N·ΔΦ/Δt |
| Energi alternatif | 20 | Sel surya, panel surya, PLTB, PLTA, PLTN, mobil listrik (P = V×I, P = E/t) |
| Pewarisan sifat | 20 | Monohibrid 3:1 dan 1:2:1, dihibrid 9:3:3:1 (saat ini dikunci) |

60 soal memakai angka acak, jadi angkanya berubah setiap kali dibuka.

## Isi paket

- `index.html` : seluruh aplikasi (siswa dan admin)
- `soal.json` : daftar soal yang dipakai semua siswa
- `manifest.webmanifest`, `sw.js`, `icon-*.png` : supaya bisa dipasang di layar utama HP dan tetap terbuka saat offline

## Memasang di internet (gratis)

Aplikasi ini berkas statis, jadi cukup diunggah ke satu alamat web.

**Netlify (paling mudah):** buka app.netlify.com/drop, lalu seret seluruh folder ini ke halaman itu. Alamat web langsung dibuat.

**GitHub Pages:** unggah isi folder ke repositori, lalu aktifkan Pages di Settings.

Alamat web harus diawali `https://` supaya bisa dipasang sebagai aplikasi. Kedua layanan di atas sudah memakainya.

## Memasang di HP siswa

- **Android (Chrome):** buka alamatnya, ketuk menu titik tiga, pilih *Instal aplikasi* atau *Tambahkan ke layar utama*.
- **iPhone (Safari):** ketuk tombol bagikan, pilih *Tambahkan ke Layar Utama*.

## Mengelola soal (mode admin)

1. Buka **Pengaturan** (ikon roda gigi di beranda), lalu **Masuk mode guru**, dan masukkan PIN guru. PIN bisa diganti di menu Pengaturan mode guru. Setelah diganti, upload soal.json (lewat Ekspor) supaya PIN baru berlaku di semua HP.
2. Tambah, ubah, gandakan, urutkan, atau hapus soal. Tombol **Coba soal ini** di formulir menampilkan soal seperti yang dilihat siswa sebelum disimpan.
3. Perubahan tersimpan di peramban perangkat guru. Supaya semua siswa memakai soal terbaru:
   - Buka **Ekspor**, lalu ketuk **Unduh soal.json** (atau **Bagikan berkas** di HP).
   - Di GitHub: **Add file → Upload files**, pilih berkas itu, lalu **Commit changes**. Pastikan namanya tepat `soal.json`, bukan `soal (1).json`.
4. Siswa mendapat soal baru saat membuka aplikasi lagi dengan internet. Salinan lama di HP siswa otomatis diganti kalau soal.json di internet lebih baru.

**Kunci materi:** di Pengaturan mode guru, ketuk nama materi untuk mengunci atau membukanya. Materi yang dikunci tetap tampil di beranda, tetapi saat diketuk muncul pesan "Soal ini belum bisa dibuka." Upload soal.json lewat Ekspor supaya berlaku di HP siswa.

**Tampil di kelas:** tab di mode guru untuk proyektor atau TV. Pilih materi, lalu **Mulai tampil**. Langkah dibuka satu per satu: baca cerita, pilah besaran, tebak materi (A–D), tebak rumus (A–D), hitung, jawaban. Ada tombol angka baru, pengatur waktu 30 detik, dan soal berikutnya. Dari laptop: → atau spasi lanjut, ← mundur, N angka baru, T waktu, F layar penuh, Esc keluar.

PIN disimpan tersandi di soal.json, bukan angka aslinya. PIN adalah kunci ringan untuk mode guru, bukan pengaman data, karena aplikasi berjalan sepenuhnya di peramban.

## Tingkat bantuan (untuk siswa)

Di halaman utama, siswa memilih tingkat:

- **Mudah**: sebagian kotak sudah terisi.
- **Menengah**: kotak kosong, satuan tampil sebagai petunjuk.
- **Sulit**: kotak kosong tanpa petunjuk satuan, soal cerita murni.

## Langkah latihan

1. **Pilah cerita**: siswa menaruh angka ke kotak besaran dan mencoret info pengecoh.
2. **Kenali materi**: siswa menentukan apakah soal termasuk materi yang mana (Listrik, Magnet, Energi alternatif, dan materi lain yang terbuka). Di tingkat Mudah dan Menengah, setiap pilihan disertai penjelasan singkat. Nama materi di bawah judul dan kartu rumus baru muncul setelah langkah ini benar.
3. **Pilih rumus**.
4. **Hitung**.

## Alat belajar untuk siswa

Di halaman utama ada kotak **Alat belajar**:

- **Catatanku**: kesalahan yang sering muncul (tertukar besaran, memakai pengecoh, lupa konversi, salah rumus) beserta tipsnya. Catatan hanya tersimpan di HP siswa.
- **Laporan ke guru**: siswa mengisi nama dan kelas, lalu menekan **Kirim lewat WhatsApp** atau **Salin teks**. Laporan berisi soal yang selesai, kesalahan teratas, dan hasil ujian terakhir.
- **Naik tingkat otomatis**: setelah 3 soal lancar berturut-turut (paling banyak 1 kesalahan), aplikasi menyarankan naik tingkat.
- **Kartu rumus**: rumus dan satuan setiap materi yang terbuka, ditambah daftar konversi. Saat latihan, kartu ini bisa dibuka di bawah langkah 1. Kartu disembunyikan di tingkat Sulit dan saat ujian.
- **Mode ujian**: 5 atau 10 soal acak dari kelas yang dipilih, dengan pengatur waktu. Siswa bisa melewati soal, dan ringkasan hasil tampil di akhir.
- **Bacakan soal**: tombol suara yang membacakan cerita soal, memakai suara bawaan HP.
- **Latihan konversi**: 10 soal cepat seperti g→kg, mA→A, menit→s, W→kW, dan cm²→m².
- **Kuis simbol dan satuan**: 10 pertanyaan cepat tentang simbol dan satuan dari Bab 5.
- **Ulangi soal yang pernah salah**: daftar soal yang masih ada salahnya. Soal hilang dari daftar setelah diselesaikan tanpa salah.
- **Buat soal sendiri**: siswa diberi rumus, angka, dan yang ditanya, lalu menulis cerita soalnya sendiri. Aplikasi memeriksa apakah semua angka dipakai dan ada kalimat tanya, lalu siswa menjawab soal buatannya. Cerita yang lengkap bisa disimpan di Ceritaku dan dikirim ke guru lewat WhatsApp.
- **Ringkasan soal**: setelah soal selesai, tampil ringkasan besaran, materi, rumus, dan hasil.
- **Pengenalan**: tiga layar singkat untuk siswa baru. Bisa dibuka lagi dari Pengaturan.
- **Pengaturan**: nama dan kelas, tema (ikuti HP, terang, gelap), ukuran teks, hapus data, dan pintu masuk mode guru.

## Soal variasi (angka acak)

Angka di cerita bisa dibuat acak setiap soal dibuka, supaya siswa tidak bisa sekadar menyalin jawaban temannya.

1. Di formulir soal, isi bagian **Variasi angka**: nama (huruf tanpa spasi), min, maks, dan langkah. Contoh: `massa`, 100, 500, 100.
2. Tulis nama itu di cerita dan besaran dalam kurung kurawal, misalnya `{massa} g`.
3. Untuk konversi, buat variasi turunan dengan rumus, misalnya `mkg` = `massa/1000`.
4. Di **Rumus jawaban**, tulis rumusnya dengan nama variasi, misalnya `mkg * 10 * tinggi`.

Kalau soal tidak memakai variasi, kosongkan Rumus jawaban dan isi jawaban benar seperti biasa.

## Cara menulis soal yang baik

- Isi **Kelas** (7, 8, atau 9). Soal ditampilkan di halaman kelas yang sesuai.
- Tulis cerita lengkap, termasuk kalimat pertanyaan.
- Untuk tiap besaran, ketik teks **persis seperti di cerita** (contoh: `200 g`), lalu isi simbol, nama, dan satuan yang dipakai di rumus.
- Isi **Hasil konversi** bila satuan di cerita berbeda dari satuan rumus (contoh: `= 0,2 kg`).
- Tambahkan info pengecoh (harga, umur, nama) supaya siswa berlatih memilah.
- Isi dua sampai empat pilihan rumus. Tandai satu yang benar dan tulis alasan untuk tiap pilihan.
- Isi *Kesalahan yang sering muncul* dengan angka yang biasa dijawab siswa, supaya petunjuknya tepat.
