# TakTik IPA

Latihan soal cerita IPA SMP kelas 7, 8, dan 9. Siswa mengubah kalimat soal menjadi besaran, mengenali materinya, memilih rumus, lalu menghitung. Guru mengelola soal lewat **Pengaturan → Masuk mode guru**.

## Isi soal

111 soal yang mengikuti Buku IPA SMP/MTs Kurikulum Merdeka.

**Kelas 7 (15 soal)**

| Materi | Jumlah | Isi |
|---|---|---|
| Massa jenis | 5 | ρ = m ÷ V, mencari massa dan volume, konversi liter ke cm³ |
| Suhu dan kalor | 5 | Q = m × c × ΔT, konversi gram ke kg, Celsius ke kelvin dan Fahrenheit |
| Gerak | 5 | v = s ÷ t, jarak, waktu, konversi km/jam ke m/s dan menit ke sekon |

**Kelas 8 (15 soal)**

| Materi | Jumlah | Isi |
|---|---|---|
| Tekanan | 5 | P = F ÷ A, tekanan hidrostatis Ph = ρ × g × h, dongkrak hidrolik (hukum Pascal) |
| Usaha dan energi | 5 | W = F × s, Ep = m × g × h, Ek = ½ × m × v², daya, tuas |
| Getaran dan gelombang | 5 | f = n ÷ t, T = t ÷ n, v = λ × f, bunyi pantul d = v × t ÷ 2 |

**Kelas 9 (81 soal)**

| Materi | Jumlah | Isi |
|---|---|---|
| Listrik | 21 | Coulomb, medan dan beda potensial, I = q/t, hukum Ohm, seri, paralel, hambatan jenis, daya dan energi |
| Magnet | 20 | Gaya Lorentz F = I·L·B sin θ, GGL induksi ε = N·ΔΦ/Δt |
| Energi alternatif | 20 | Sel surya, panel surya, PLTB, PLTA, PLTN, mobil listrik (P = V×I, P = E/t) |
| Pewarisan sifat | 20 | Monohibrid 3:1 dan 1:2:1, dihibrid 9:3:3:1 (saat ini dikunci) |

90 soal memakai angka acak, jadi angkanya berubah setiap kali dibuka. Semua soal kelas 7 dan 8 memakai angka acak.

## Isi paket

- `index.html` : seluruh aplikasi (siswa dan admin)
- `soal.json` : daftar soal yang dipakai semua siswa
- `manifest.webmanifest`, `sw.js`, `icon-*.png` : supaya bisa dipasang di layar utama HP dan tetap terbuka saat offline
- `server/worker.js`, `server/wrangler.toml` : server soal di Cloudflare (lihat bagian Server soal)

## Memasang di internet (gratis)

Aplikasi ini berkas statis, jadi cukup diunggah ke satu alamat web.

**Netlify (paling mudah):** buka app.netlify.com/drop, lalu seret seluruh folder ini ke halaman itu. Alamat web langsung dibuat.

**GitHub Pages:** unggah isi folder ke repositori, lalu aktifkan Pages di Settings.

Alamat web harus diawali `https://` supaya bisa dipasang sebagai aplikasi. Kedua layanan di atas sudah memakainya.

## Memasang di HP siswa (Android dan iPhone)

TakTik IPA adalah aplikasi web yang bisa dipasang (PWA). Setelah dipasang, ikonnya ada di layar utama, terbuka layar penuh seperti aplikasi biasa, dan tetap bisa dipakai tanpa internet.

- **Android (Chrome):** buka alamatnya. Di beranda aplikasi muncul kotak **Pasang di HP**, ketuk **Pasang aplikasi**. Bisa juga lewat menu titik tiga → *Instal aplikasi*.
- **iPhone/iPad (Safari):** ketuk tombol bagikan (kotak dengan panah ke atas), pilih *Tambahkan ke Layar Utama*. Petunjuk yang sama tampil di dalam aplikasi.

Cara pasang juga ada di **Pengaturan → Pasang di HP**.

## Memperbarui aplikasi (tanpa uninstall)

Seperti aplikasi dari Play Store, TakTik IPA memperbarui dirinya sendiri. Siswa tidak perlu menghapus atau memasang ulang, dan kemajuan belajar di HP tetap tersimpan.

1. Ubah berkas di GitHub (misalnya `index.html`).
2. Naikkan nomor versi di **dua** tempat dengan angka yang sama:
   - `sw.js` : baris `const VERSI = '2.1.0';`
   - `index.html` : cari `APP_VERSI='2.1.0'`
   Contoh: `2.1.0` → `2.1.1` untuk perbaikan kecil, `2.2.0` untuk fitur baru.
   Untuk fitur baru, tulis juga ringkasannya di `BARU` (cari `var BARU=` di `index.html`). Isinya tampil sekali di beranda sebagai kotak **Baru di versi ini**.
3. Commit. Saat siswa membuka aplikasi dengan internet, versi baru langsung terpasang. Kalau siswa sedang mengerjakan soal, muncul tulisan **Versi baru siap** dengan tombol **Perbarui**, jadi pekerjaannya tidak hilang.

Nomor versi yang sedang terpasang terlihat di **Pengaturan → Aplikasi**, beserta tombol **Periksa pembaruan**.

Soal (soal.json atau server) tidak perlu menaikkan versi; soal baru selalu diambil setiap kali aplikasi dibuka.

### Aplikasi Android (APK)

APK dibuat otomatis oleh GitHub Actions (`.github/workflows/taktik.yml`) dari folder `taktik/`, lalu diterbitkan di Release `taktik`:
https://github.com/Tendiriana/cerita-jadi-simbol/releases/download/taktik/TakTik-IPA.apk

- APK membuka versi web, jadi perubahan `index.html` atau `soal.json` langsung sampai tanpa APK baru.
- APK baru hanya dibuat kalau folder `taktik/` atau workflow-nya berubah. Bisa juga dibuat manual: tab **Actions** → **Build TakTik IPA APK** → **Run workflow**.
- Di **Pengaturan → Aplikasi**, *Versi* adalah versi isi aplikasi (selalu terbaru), sedangkan *Aplikasi Android* adalah versi APK.
- APK ditandatangani kunci dari secret `TAKTIK_KEYSTORE` dan `TAKTIK_KEY_PASSWORD`. Selama kuncinya sama, APK baru bisa dipasang menimpa yang lama tanpa uninstall. Simpan cadangan kuncinya.

### Alternatif APK lewat PWABuilder (opsional)

Buka pwabuilder.com, masukkan alamat web TakTik IPA, lalu pilih **Android → Generate**. Hasilnya berkas APK/AAB yang membuka alamat web itu sebagai aplikasi. Karena isinya tetap diambil dari web, setiap pembaruan di GitHub langsung sampai ke HP tanpa memasang APK baru. APK yang sama juga bisa diunggah ke Play Store. iPhone tidak bisa memasang APK; pakai cara *Tambahkan ke Layar Utama* di atas.

## Panduan guru (mode guru)

Buka **Pengaturan** (ikon roda gigi) → **Masuk mode guru**, lalu masukkan PIN. Centang **Ingat saya di HP ini** supaya tidak perlu mengetik PIN lagi. Matikan centang itu kalau HP-nya juga dipakai siswa.

Menu guru berisi 4 tombol besar:

| Tombol | Kegunaan |
|---|---|
| **➕ Tambah soal** | Pilih rumus, isi angka, tulis cerita. Selesai. |
| **📚 Soal saya** | Daftar soal per kelas: coba, ubah cerita, hapus, urutkan |
| **📺 Tampil di kelas** | Bahas soal di proyektor atau TV |
| **⚙️ Pengaturan kelas** | Kunci kelas (7, 8, 9) atau materi tertentu, ganti PIN, dan alamat server tambahan |
| **📝 Ujian kelas** | Buat ujian berkode, pantau siswa, kumpulkan jawaban, dan beri nilai |

Tombol **⋮** di kanan atas berisi **Cara pakai**: panduan guru di mode guru, dan panduan siswa di halaman siswa.

Di atas layar selalu ada keterangan **✓ Semua soal sudah terkirim ke siswa**. Setiap soal yang disimpan, diubah, atau dihapus **langsung terkirim** ke siswa. Tidak ada langkah kirim yang terpisah.

### Tambah soal (tanpa mengetik rumus)

1. **Pilih rumus.** Pilih kelas, lalu pilih rumusnya, misalnya *Hukum Ohm: mencari kuat arus (I = V ÷ R)*. Tersedia 44 rumus untuk Kelas 7–9.
2. **Isi angkanya.** Jawabannya langsung dihitung oleh aplikasi. Biarkan **Angka acak untuk tiap siswa** tercentang supaya angka di soal berubah setiap kali dibuka.
3. **Tulis ceritanya** seperti menulis di papan tulis. Semua angka dari langkah 2 harus ada di cerita beserta satuannya, dan cerita diakhiri pertanyaan "Berapa …?". Angka lain, misalnya harga atau jam, otomatis menjadi pengecoh. Ada tombol **Pakai contoh cerita** dan **✨ Tuliskan dengan AI** kalau sedang sibuk.
4. Ketuk **Periksa cerita**. Aplikasi menunjukkan angka yang dikenali, kalimat tanya, dan pengecohnya. Kalau ada yang kurang, muncul pesan yang jelas, misalnya "Angka 220 V belum ada di cerita".
5. Ketuk **Coba seperti siswa** untuk melihat hasilnya, lalu **Simpan & kirim ke siswa**.

Rumusnya tidak ada di daftar? Pakai **formulir lengkap** (tautan di bawah daftar rumus, atau tombol **Formulir** di Soal saya).

Untuk membuat soal yang perlu konversi satuan, pilih satuan lain di samping angka, misalnya **g** untuk massa, **menit** atau **jam** untuk waktu, **cm** untuk panjang, atau **mA** untuk arus. Aplikasi otomatis menambahkan langkah konversi (contoh: 200 g = 0,2 kg), tips di kotak besaran, dan pesan khusus kalau siswa lupa mengubah satuan.

### Ujian kelas

1. **Ujian kelas → Buat ujian baru.** Isi judul, pilih kelas dan materi (boleh lebih dari satu), jumlah soal, dan lama ujian.
2. Atur aturannya:
   - tingkat bantuan (bawaan: Sulit),
   - acak urutan soal,
   - boleh melewati soal,
   - tampilkan skor ke siswa setelah selesai.
3. Ketuk **Buat & bagikan**. Muncul **kode ujian** 6 huruf. Bagikan lewat WhatsApp atau tulis di papan.
4. Siswa membuka menu **Ujian** di bawah layar, mengisi kode dan nama, lalu mengerjakan. Selama ujian berlangsung, beranda menampilkan kartu **Lanjutkan ujian**. Waktu setiap siswa berjalan sejak ia mengetuk Mulai ujian.
5. Jawaban tersimpan setiap kali satu soal selesai. Jawaban terkumpul otomatis saat waktu habis, saat siswa mengakhiri ujian, atau saat guru menekan **Tutup ujian**. Kalau HP siswa tertutup, ujian bisa dilanjutkan.
6. Daftar siswa diperbarui setiap 15 detik. Ketuk nama siswa untuk melihat jawabannya per soal (jawaban siswa, kunci, jumlah kesalahan, waktu) dan memberi **nilai akhir**. Nilai saran sudah dihitung otomatis.
7. **Salin daftar nilai** untuk ditempel ke WhatsApp atau Excel. **Hapus lembar kerja** membuat siswa itu boleh mengulang.

Ujian kelas memerlukan **server ujian** (lihat *Server ujian* di bawah).

### Ubah cerita soal

Di **Soal saya**, ketuk **Ubah cerita** untuk mengganti cerita dengan konteks sekolah atau daerah sendiri. Rumus dan jawabannya tetap sama. Tanda seperti `[m]` adalah angka yang berubah setiap soal dibuka, jadi jangan dihapus.

### AI

Untuk rumus yang ada di daftar aplikasi, AI hanya **memilih rumus, memilih angka, dan menulis cerita**. Rumus, pilihan jawaban, dan jawaban dirakit oleh aplikasi, jadi tidak bisa salah. Untuk rumus yang tidak ada di daftar, misalnya gaya Coulomb, AI menulis **soal lengkap**. Soal itu tetap diperiksa dan jawabannya dihitung ulang oleh aplikasi sebelum bisa ditambahkan. Kalau soal dari AI belum lolos pemeriksaan, aplikasi meminta AI memperbaikinya secara otomatis.

- Di **Tambah soal**: tombol **✨ Tuliskan dengan AI** menulis cerita untuk rumus yang dipilih. Tema boleh diisi, misalnya "kipas angin di masjid".
- Di **Lainnya → ✨ Ngobrol dengan AI**: minta beberapa soal sekaligus dengan kata-kata biasa, misalnya "Buat 3 soal hukum Ohm tentang alat listrik di rumah".

Supaya hasil AI bagus, pasang **server AI dengan Claude** (lihat bagian *Server AI* di bawah). Tanpa server AI, aplikasi memakai AI bawaan server soal yang kualitasnya lebih rendah.

**Kunci kelas dan materi:** di **Pengaturan kelas**, ketuk **Kunci kelas** untuk menutup satu kelas sekaligus. Ketuk nama kelas untuk melihat materinya, lalu ketuk materi untuk mengunci atau membukanya. Materi yang dikunci tetap tampil di beranda, tetapi saat diketuk muncul pesan "Soal ini belum bisa dibuka." Upload soal.json lewat Ekspor supaya berlaku di HP siswa.

**Tampil di kelas:** tab di mode guru untuk proyektor atau TV. Pilih materi, lalu **Mulai tampil**. Langkah dibuka satu per satu: baca cerita, pilah besaran, tebak materi (A–D), tebak rumus (A–D), hitung, jawaban. Ada tombol angka baru, pengatur waktu 30 detik, dan soal berikutnya. Dari laptop: → atau spasi lanjut, ← mundur, N angka baru, T waktu, F layar penuh, Esc keluar.

PIN disimpan tersandi di soal.json, bukan angka aslinya. PIN adalah kunci ringan untuk mode guru, bukan pengaman data, karena aplikasi berjalan sepenuhnya di peramban.

## Server soal dan kode kelas

Soal dikirim lewat server Cloudflare (`server/worker.js`), jadi guru tidak perlu mengunduh atau mengupload berkas.

- **Pengelola utama**: masuk mode guru dengan PIN utama, ubah soal, lalu ketuk **Kirim ke siswa**. Tab **Kelas** dipakai untuk membuat kode kelas guru lain, mengganti PIN guru, dan menghapus kelas.
- **Guru kelas**: isi kode kelas di **Pengaturan → Kode kelas**, lalu masuk mode guru dengan PIN guru. Perubahan guru kelas tidak mengubah soal utama.
- **Siswa**: isi kode kelas dari guru di Pengaturan. Tanpa kode, siswa memakai soal utama. Soal terakhir tetap tersimpan untuk dipakai tanpa internet.
- PIN dicek di server. Setelah 10 kali PIN salah, percobaan dikunci 15 menit.

- **Buat dengan AI**: di mode guru, guru menulis permintaan soal seperti mengobrol. AI (Cloudflare Workers AI, gratis dengan kuota harian) menyusun soal lengkap, aplikasi memeriksa dan menghitung ulang jawabannya, lalu guru menekan **Tambahkan & kirim ke siswa**. Batas 40 permintaan per kode kelas per hari.

### Server ujian

`server/ujian-worker.js` adalah server terpisah yang menyimpan ujian dan jawaban siswa di database D1 Cloudflare. Gratis sampai 100.000 penulisan per hari. Server soal yang sudah berjalan tidak perlu diubah.

1. Dasbor Cloudflare: **Storage & Databases → D1 → Create**, beri nama `taktik-ujian`.
2. **Workers & Pages → Create → Worker**, beri nama `taktik-ujian`, lalu **Deploy**. Ketuk **Edit code**, tempel seluruh isi `server/ujian-worker.js`, lalu **Deploy**.
3. Di worker itu: **Settings → Bindings → Add → D1 database**, nama variabel `DB`, pilih database `taktik-ujian`.
4. Salin alamat worker, misalnya `https://taktik-ujian.<nama>.workers.dev`.
5. Di aplikasi: mode guru → **Pengaturan kelas → Server tambahan**, tempel alamatnya, lalu ketuk **Periksa & simpan**. Alamat itu ikut terkirim ke semua HP siswa.

Tabel database dibuat otomatis saat server pertama kali dipakai. Server AI juga bisa disambungkan lewat kolom yang sama.

### Server AI (Claude)

`server/ai-worker.js` adalah server terpisah khusus untuk AI. Server soal yang sudah berjalan tidak perlu diubah, karena PIN guru dicek ke server soal lewat `/login`.

1. Buat kunci API di console.anthropic.com → **API Keys**, lalu isi saldo (**Billing**). Pemakaian dibayar per permintaan.
2. Di dasbor Cloudflare: **Workers & Pages → Create → Worker**, beri nama `taktik-ai`, ketuk **Deploy**, lalu **Edit code**. Tempel seluruh isi `server/ai-worker.js` dan ketuk **Deploy**.
3. Buka **Settings → Variables and Secrets**, tambahkan secret `ANTHROPIC_API_KEY` berisi kunci dari langkah 1.
4. (Opsional) Tambahkan binding KV bernama `DB` supaya kuota 40 permintaan per hari per kelas berlaku.
5. Salin alamat worker, misalnya `https://taktik-ai.<nama>.workers.dev`, lalu tempel di mode guru → **Pengaturan kelas → Server tambahan**.

Model bawaan adalah `claude-opus-5-5` (kualitas terbaik). Untuk biaya lebih murah, isi variabel `CLAUDE_MODEL`, misalnya `claude-sonnet-5-5` atau `claude-haiku-5-5`.

Pemasangan server: Worker `taktik-api` dengan binding KV bernama `DB`, binding Workers AI bernama `AI`, dan secret `ADMIN_PIN`. Kodenya ada di `server/worker.js`, pengaturannya di `server/wrangler.toml`. Alamat server ditulis di `index.html` (`API_URL`).

`server/worker.js` disusun ulang dari cara aplikasi memanggil server. Kalau server lama masih berjalan normal, tidak perlu dipasang ulang. Memasang kode ini di atas server lama membuat PIN guru kelas yang lama tidak berlaku (cara penyandiannya bisa berbeda), jadi setelah memasang, buat ulang PIN guru lewat tab **Kelas**.

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
- **Ujian dari guru** (menu Ujian): masukkan kode ujian dari guru dan nama, lalu kerjakan. Waktu berjalan mundur, dan jawaban terkumpul otomatis ke guru.
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
