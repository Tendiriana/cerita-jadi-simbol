// TakTik IPA — server soal (Cloudflare Worker).
// Binding: KV "DB", Workers AI "AI", secret "ADMIN_PIN".
// Isi KV:
//   bank:<KODE>    → {nama, rev, kunci, soal, pin}   (pin = hash PIN guru; UTAMA memakai ADMIN_PIN)
//   pin:UTAMA      → hash PIN utama kalau sudah diganti lewat aplikasi
//   gagal:<KODE>:<ip> → jumlah PIN salah (kedaluwarsa 15 menit)
//   ai:<KODE>:<tgl>   → jumlah permintaan AI hari itu

const UTAMA = 'UTAMA';
const MAX_GAGAL = 10, KUNCI_DETIK = 15 * 60;
const AI_HARIAN = 40;
const AI_MODEL = '@cf/meta/llama-3.3-70b-instruct-fp8-fast';
const MAX_SOAL = 500, MAX_BYTES = 4 * 1024 * 1024;

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Access-Control-Max-Age': '86400'
};
const json = (o, status = 200) => new Response(JSON.stringify(o), {
  status, headers: { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', ...CORS }
});
const gagal = (status, pesan) => json({ pesan }, status);

const normKode = v => String(v == null ? '' : v).trim().toUpperCase().replace(/\s+/g, '-');
const kodeOk = v => /^[A-Z0-9-]{3,20}$/.test(v);

async function hash(pin) {
  const d = await crypto.subtle.digest('SHA-256', new TextEncoder().encode('taktik:' + String(pin == null ? '' : pin).trim()));
  return [...new Uint8Array(d)].map(b => b.toString(16).padStart(2, '0')).join('');
}
function samaRata(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) return false;
  let x = 0;
  for (let i = 0; i < a.length; i++) x |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return x === 0;
}

const getBank = (env, kode) => env.DB.get('bank:' + kode, 'json');
async function putBank(env, kode, bank) {
  await env.DB.put('bank:' + kode, JSON.stringify(bank), {
    metadata: { nama: bank.nama || '', n: (bank.soal || []).length, rev: bank.rev || 0 }
  });
}

async function pinUtamaOk(env, pin) {
  const h = await hash(pin), simpan = await env.DB.get('pin:' + UTAMA);
  if (simpan) return samaRata(h, simpan);
  return !!env.ADMIN_PIN && samaRata(h, await hash(env.ADMIN_PIN));
}

/* PIN salah dibatasi per kode dan per alamat IP */
async function cekKunci(env, req, kode) {
  const ip = req.headers.get('CF-Connecting-IP') || 'x';
  const key = 'gagal:' + kode + ':' + ip;
  const n = +(await env.DB.get(key)) || 0;
  return {
    terkunci: n >= MAX_GAGAL,
    salah: () => env.DB.put(key, String(n + 1), { expirationTtl: KUNCI_DETIK }),
    benar: () => (n ? env.DB.delete(key) : null)
  };
}

/* Mengembalikan peran ('utama' atau 'guru') kalau PIN cocok, atau Response kalau gagal */
async function masuk(env, req, kodeMentah, pin) {
  const kode = normKode(kodeMentah) || UTAMA;
  if (!kodeOk(kode)) return gagal(400, 'Kode kelas tidak valid.');
  const k = await cekKunci(env, req, kode);
  if (k.terkunci) return gagal(429, 'Terlalu banyak PIN salah. Coba lagi 15 menit lagi.');
  let peran = null;
  if (await pinUtamaOk(env, pin)) peran = 'utama';
  else if (kode !== UTAMA) {
    const bank = await getBank(env, kode);
    if (!bank) return gagal(404, 'Kode kelas tidak ditemukan.');
    if (bank.pin && samaRata(await hash(pin), bank.pin)) peran = 'guru';
  }
  if (!peran) { await k.salah(); return gagal(401, 'PIN salah. Coba lagi.'); }
  await k.benar();
  return { peran, kode };
}
async function masukUtama(env, req, pin) {
  const r = await masuk(env, req, UTAMA, pin);
  if (r instanceof Response) return r;
  return r.peran === 'utama' ? r : gagal(403, 'Hanya pengelola utama yang bisa melakukan ini.');
}

function rapikanSoal(soal, kunci) {
  if (!Array.isArray(soal)) return { err: 'Daftar soal tidak valid.' };
  if (soal.length > MAX_SOAL) return { err: 'Soal terlalu banyak (maksimal ' + MAX_SOAL + ').' };
  if (soal.some(s => !s || typeof s !== 'object' || Array.isArray(s))) return { err: 'Ada soal yang rusak.' };
  const k = Array.isArray(kunci) ? kunci.filter(x => typeof x === 'string').slice(0, 50) : [];
  if (JSON.stringify(soal).length > MAX_BYTES) return { err: 'Ukuran soal terlalu besar.' };
  return { soal, kunci: k };
}

/* ---------- AI ---------- */
const SISTEM = `Kamu membantu guru IPA SMP di Indonesia (Kurikulum Merdeka) membuat soal cerita hitungan untuk aplikasi TakTik IPA.
Siswa memilah angka di cerita ke kotak besaran, mencoret info pengecoh, mengenali materi, memilih rumus, lalu menghitung.

Balas dalam bahasa Indonesia yang singkat. Setiap soal ditulis di SATU blok \`\`\`json berisi {"soal":[ ... ]} (paling banyak 3 soal). Format tiap soal:
{
 "kelas":"7"|"8"|"9",
 "topik":"nama materi, pakai nama materi yang sudah ada kalau cocok",
 "judul":"judul pendek",
 "cerita":"cerita lengkap, termasuk kalimat pertanyaan yang diawali Berapa",
 "variasi":[{"nama":"massa","min":100,"maks":500,"langkah":100}],
 "besaran":[{"teks":"teks PERSIS seperti di cerita, mis. 200 g","sym":"m","nama":"massa","satuan":"kg","ket":"penjelasan singkat","konv":"= 0,2 kg (isi kalau satuan berbeda)","catatan":""}],
 "pengecoh":[{"teks":"teks persis di cerita, mis. Rp15.000","ket":"kenapa tidak dipakai"}],
 "tanya":{"teks":"potongan kalimat pertanyaan persis di cerita"},
 "rumus":[{"f":"P = V × I","benar":true,"alasan":"..."},{"f":"P = V ÷ I","benar":false,"alasan":"..."}],
 "hitung":{"diketahui":"V = 12 V · I = 2 A","lhs":"P","ekspresi":"12 × 2","satuan":"W","jawaban":24,"angka":"",
   "salah":[{"nilai":6,"pesan":"Itu hasil bagi. Daya adalah tegangan kali arus."}],
   "petunjuk":"Kalikan tegangan dengan kuat arus.","penjelasan":"P = V × I = 12 × 2 = 24 W."}
}
Aturan penting:
- Setiap "teks" besaran, pengecoh, dan tanya harus muncul persis di cerita dan tidak saling bertumpukan.
- Simbol besaran tidak boleh dipakai dua kali. Tepat satu rumus benar, 2–4 pilihan rumus.
- Pakai koma untuk desimal di cerita (0,5 A). Di "ekspresi" boleh pakai × ÷ dan kurung.
- Kalau memakai variasi angka: tulis {nama} di cerita, teks besaran, diketahui, ekspresi, dan penjelasan; isi "angka" dengan rumus jawaban memakai nama variasi (mis. "V*I"), kosongkan "jawaban". Untuk konversi buat variasi turunan dengan "rumus", mis. {"nama":"mkg","rumus":"massa/1000"}. Atur rentang supaya jawaban paling banyak 2 angka desimal.
- "salah" berisi jawaban keliru yang sering muncul; kalau memakai variasi, isi "rumus" (mis. "V/I") sebagai ganti "nilai".
- Tambahkan satu pengecoh yang wajar (harga, nama, waktu yang tidak dipakai, warna).
- Konteks dekat dengan kehidupan siswa Indonesia. Hitung dengan teliti.`;

async function tanyaAI(env, kode, pesan, konteks) {
  const tgl = new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10);
  const key = 'ai:' + kode + ':' + tgl;
  const n = +(await env.DB.get(key)) || 0;
  if (n >= AI_HARIAN) return gagal(429, 'Kuota AI hari ini sudah habis (' + AI_HARIAN + ' permintaan). Coba lagi besok.');
  if (!env.AI) return gagal(503, 'AI belum dipasang di server.');
  const k = konteks && typeof konteks === 'object' ? konteks : {};
  const info = 'Kelas yang sedang dibuka: ' + (k.kelas || '9') + '. Materi yang sudah ada: ' + (Array.isArray(k.materi) ? k.materi.join(', ') : '-')
    + '. Materi yang dikunci: ' + (Array.isArray(k.kunci) && k.kunci.length ? k.kunci.join(', ') : '-')
    + '. Judul soal yang sudah ada (jangan dibuat sama): ' + (Array.isArray(k.judul) ? k.judul.slice(0, 120).join('; ') : '-') + '.';
  const riwayat = (Array.isArray(pesan) ? pesan : []).slice(-8)
    .filter(m => m && (m.role === 'user' || m.role === 'assistant') && typeof m.content === 'string')
    .map(m => ({ role: m.role, content: m.content.slice(0, 12000) }));
  if (!riwayat.length || riwayat[riwayat.length - 1].role !== 'user') return gagal(400, 'Pesan kosong.');
  await env.DB.put(key, String(n + 1), { expirationTtl: 2 * 86400 });
  let out;
  try {
    out = await env.AI.run(AI_MODEL, { messages: [{ role: 'system', content: SISTEM + '\n\n' + info }, ...riwayat], max_tokens: 4096, temperature: 0.4 });
  } catch (e) {
    return gagal(502, 'AI sedang sibuk. Coba lagi sebentar lagi.');
  }
  const balasan = out && (out.response || (out.result && out.result.response));
  if (!balasan) return gagal(502, 'AI tidak memberi jawaban. Coba lagi.');
  return json({ balasan: String(balasan), sisa: AI_HARIAN - n - 1 });
}

/* ---------- Rute ---------- */
async function rute(req, env) {
  const url = new URL(req.url), p = url.pathname.replace(/\/+$/, '') || '/';
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

  if (req.method === 'GET' && p === '/bank') {
    const kode = normKode(url.searchParams.get('kode')) || UTAMA;
    if (!kodeOk(kode)) return gagal(400, 'Kode kelas tidak valid.');
    const b = await getBank(env, kode);
    if (!b) return gagal(404, 'Kode kelas tidak ditemukan.');
    return json({ kode, nama: b.nama || '', rev: b.rev || 0, kunci: b.kunci || [], soal: b.soal || [] });
  }
  if (req.method === 'GET' && p === '/') return json({ nama: 'taktik-api', ok: true });
  if (req.method !== 'POST') return gagal(404, 'Alamat tidak dikenal.');

  let b;
  try { b = await req.json(); } catch (e) { return gagal(400, 'Data tidak bisa dibaca.'); }
  if (!b || typeof b !== 'object') return gagal(400, 'Data tidak bisa dibaca.');

  if (p === '/login') {
    const r = await masuk(env, req, b.kode, b.pin);
    return r instanceof Response ? r : json({ peran: r.peran });
  }
  if (p === '/simpan') {
    const r = await masuk(env, req, b.kode, b.pin);
    if (r instanceof Response) return r;
    const s = rapikanSoal(b.soal, b.kunci);
    if (s.err) return gagal(400, s.err);
    const lama = (await getBank(env, r.kode)) || {};
    if (r.kode !== UTAMA && !lama.soal) return gagal(404, 'Kode kelas tidak ditemukan.');
    const rev = Date.now();
    await putBank(env, r.kode, { ...lama, rev, kunci: s.kunci, soal: s.soal });
    return json({ rev });
  }
  if (p === '/pin') {
    const r = await masuk(env, req, b.kode, b.pin);
    if (r instanceof Response) return r;
    if (String(b.baru || '').trim().length < 4) return gagal(400, 'PIN minimal 4 karakter.');
    if (r.kode === UTAMA) {
      await env.DB.put('pin:' + UTAMA, await hash(b.baru));
    } else {
      const bank = await getBank(env, r.kode);
      bank.pin = await hash(b.baru);
      await putBank(env, r.kode, bank);
    }
    return json({ ok: true });
  }
  if (p === '/ai') {
    const r = await masuk(env, req, b.kode, b.pin);
    if (r instanceof Response) return r;
    return tanyaAI(env, r.kode, b.pesan, b.konteks);
  }
  if (p.startsWith('/kelas/')) {
    const r = await masukUtama(env, req, b.pin);
    if (r instanceof Response) return r;
    if (p === '/kelas/daftar') {
      const kelas = [];
      let cursor;
      do {
        const l = await env.DB.list({ prefix: 'bank:', cursor });
        l.keys.forEach(k => {
          const kode = k.name.slice(5);
          if (kode !== UTAMA) kelas.push({ kode, nama: (k.metadata || {}).nama || '', n: (k.metadata || {}).n || 0, rev: (k.metadata || {}).rev || 0 });
        });
        cursor = l.list_complete ? null : l.cursor;
      } while (cursor);
      kelas.sort((a, b2) => a.kode.localeCompare(b2.kode));
      return json({ kelas });
    }
    const target = normKode(b.target);
    if (!kodeOk(target) || target === UTAMA) return gagal(400, 'Kode kelas 3–20 karakter, hanya huruf, angka, dan tanda hubung.');
    if (p === '/kelas/buat') {
      if (await getBank(env, target)) return gagal(409, 'Kode ' + target + ' sudah dipakai. Pilih kode lain.');
      if (String(b.pinGuru || '').trim().length < 4) return gagal(400, 'PIN guru minimal 4 karakter.');
      const s = rapikanSoal(b.soal, b.kunci);
      if (s.err) return gagal(400, s.err);
      const rev = Date.now();
      await putBank(env, target, { nama: String(b.nama || '').slice(0, 60), rev, kunci: s.kunci, soal: s.soal, pin: await hash(b.pinGuru) });
      return json({ kode: target, rev });
    }
    const bank = await getBank(env, target);
    if (!bank) return gagal(404, 'Kode kelas tidak ditemukan.');
    if (p === '/kelas/pin') {
      if (String(b.pinGuru || '').trim().length < 4) return gagal(400, 'PIN guru minimal 4 karakter.');
      bank.pin = await hash(b.pinGuru);
      await putBank(env, target, bank);
      return json({ ok: true });
    }
    if (p === '/kelas/hapus') {
      await env.DB.delete('bank:' + target);
      return json({ ok: true });
    }
  }
  return gagal(404, 'Alamat tidak dikenal.');
}

export default {
  async fetch(req, env) {
    try { return await rute(req, env); }
    catch (e) { return gagal(500, 'Server bermasalah. Coba lagi.'); }
  }
};
