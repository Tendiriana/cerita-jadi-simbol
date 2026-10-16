# Cerita Jadi Simbol

Latihan soal cerita IPA SMP. Siswa mengubah kalimat soal menjadi besaran, memilih rumus, lalu menghitung. Guru mengelola soal lewat tombol **Admin**.

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

1. Ketuk **Admin** di pojok kanan atas. PIN awal: `1234`. Ganti di Pengaturan.
2. Tambah, ubah, gandakan, urutkan, atau hapus soal. Tombol **Coba soal ini** di formulir menampilkan soal seperti yang dilihat siswa sebelum disimpan.
3. Perubahan tersimpan di peramban perangkat guru. Supaya semua siswa memakai soal terbaru:
   - Buka **Ekspor**, lalu unduh atau salin isinya sebagai `soal.json`.
   - Ganti berkas `soal.json` di hosting dengan berkas baru itu (di Netlify: seret folder lagi).
4. Siswa mendapat soal baru saat membuka aplikasi lagi dengan internet.

PIN hanya mencegah ubahan tak sengaja. PIN bukan pengaman data, karena aplikasi berjalan sepenuhnya di peramban.

## Cara menulis soal yang baik

- Tulis cerita lengkap, termasuk kalimat pertanyaan.
- Untuk tiap besaran, ketik teks **persis seperti di cerita** (contoh: `200 g`), lalu isi simbol, nama, dan satuan yang dipakai di rumus.
- Isi **Hasil konversi** bila satuan di cerita berbeda dari satuan rumus (contoh: `= 0,2 kg`).
- Tambahkan info pengecoh (harga, umur, nama) supaya siswa berlatih memilah.
- Isi dua sampai empat pilihan rumus. Tandai satu yang benar dan tulis alasan untuk tiap pilihan.
- Isi *Kesalahan yang sering muncul* dengan angka yang biasa dijawab siswa, supaya petunjuknya tepat.
