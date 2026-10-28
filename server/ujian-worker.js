// TakTik IPA — server ujian kelas (Cloudflare Worker + database D1).
// Server soal yang sudah berjalan tidak diubah: PIN guru dicek ke server itu lewat /login.
// Satu berkas supaya bisa ditempel langsung di dasbor Cloudflare.
//
// Pemasangan:
//   1. Storage & Databases → D1 → Create, beri nama taktik-ujian.
//   2. Workers & Pages → Create → Worker, beri nama taktik-ujian, tempel berkas ini, Deploy.
//   3. Worker → Settings → Bindings → Add → D1 database, nama variabel DB, pilih taktik-ujian.
// Tabel dibuat otomatis saat pertama dipakai.
//   4. Settings → Bindings → Add → Service binding, nama variabel API, pilih worker taktik-api
//      (wajib: Cloudflare tidak mengizinkan worker memanggil worker lain di workers.dev yang sama).
// Variabel opsional: MAIN_API (bawaan https://taktik-api.taktik-ipa.workers.dev).

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

const MAX_SOAL = 40, MAX_BYTES = 1024 * 1024, TENGGANG = 2 * 60 * 1000;
const HURUF = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // tanpa 0/O dan 1/I supaya mudah dibaca

function acakKode(n) {
  const b = new Uint8Array(n);
  crypto.getRandomValues(b);
  return [...b].map(x => HURUF[x % HURUF.length]).join('');
}
const sekarang = () => Date.now();

let siap = false;
async function siapkan(db) {
  if (siap) return;
  await db.batch([
    db.prepare(`CREATE TABLE IF NOT EXISTS ujian (room TEXT PRIMARY KEY, kode TEXT NOT NULL, judul TEXT, kelas TEXT,
      aturan TEXT, soal TEXT, status TEXT DEFAULT 'buka', dibuat INTEGER, ditutup INTEGER)`),
    db.prepare(`CREATE TABLE IF NOT EXISTS peserta (id TEXT PRIMARY KEY, room TEXT NOT NULL, nama TEXT, token TEXT,
      mulai INTEGER, batas INTEGER, data TEXT, selesai INTEGER DEFAULT 0, nilai REAL, catatan TEXT, diubah INTEGER)`),
    db.prepare(`CREATE INDEX IF NOT EXISTS peserta_room ON peserta(room)`),
    db.prepare(`CREATE INDEX IF NOT EXISTS ujian_kode ON ujian(kode)`)
  ]);
  siap = true;
}

async function cekGuru(env, kode, pin, req) {
  // Cloudflare menolak worker memanggil worker lain di workers.dev yang sama (error 1042),
  // jadi server soal dipanggil lewat service binding bernama API kalau ada.
  const api = (env.MAIN_API || 'https://taktik-api.taktik-ipa.workers.dev').replace(/\/+$/, '');
  // data pengirim asli ikut diteruskan; lewat service binding, header ini tidak terisi sendiri
  const h = { 'Content-Type': 'application/json', 'User-Agent': 'taktik-worker' };
  const ip = req && req.headers.get('CF-Connecting-IP');
  if (ip) { h['CF-Connecting-IP'] = ip; h['X-Forwarded-For'] = ip; }
  const asal = req && req.headers.get('Origin');
  if (asal) h['Origin'] = asal;
  const opsi = { method: 'POST', headers: h, body: JSON.stringify({ kode, pin }) };
  let r;
  try {
    r = env.API ? await env.API.fetch(new Request(api + '/login', opsi)) : await fetch(api + '/login', opsi);
  } catch (e) {
    return gagal(502, 'Server soal tidak bisa dihubungi (' + String(e && e.message || e).slice(0, 160) + ').');
  }
  if (r.ok) return null;
  const j = await r.json().catch(() => null);
  if (j && j.pesan && (r.status === 401 || r.status === 403 || r.status === 404 || r.status === 429)) return gagal(r.status === 429 ? 429 : 401, j.pesan);
  return gagal(502, env.API
    ? 'Server soal menjawab tidak seperti biasa (' + r.status + '). Coba lagi.'
    : 'Server ini belum tersambung ke server soal. Di Cloudflare tambahkan Service binding bernama API ke worker taktik-api.');
}
const normKode = v => String(v == null ? '' : v).trim().toUpperCase().replace(/\s+/g, '-') || 'UTAMA';
const normRoom = v => String(v == null ? '' : v).trim().toUpperCase().replace(/[^A-Z0-9]/g, '');

function aturanBersih(a) {
  a = a && typeof a === 'object' ? a : {};
  const menit = Math.min(240, Math.max(1, Math.round(+a.menit || 30)));
  return {
    menit,
    tingkat: ['mudah', 'menengah', 'sulit'].includes(a.tingkat) ? a.tingkat : 'sulit',
    acak: a.acak !== false,
    lewati: a.lewati !== false,
    lihatNilai: a.lihatNilai !== false,
    sekali: a.sekali !== false
  };
}
function statusPeserta(p, u, t) {
  if (p.selesai) return 'selesai';
  if (u.status === 'tutup' || t > p.batas) return 'waktu habis';
  return 'mengerjakan';
}
function ringkas(p, u, t) {
  let d = {};
  try { d = JSON.parse(p.data || '{}'); } catch (e) {}
  return { id: p.id, nama: p.nama, mulai: p.mulai, batas: p.batas, diubah: p.diubah, status: statusPeserta(p, u, t),
    skor: d.skor == null ? null : d.skor, benar: d.benar || 0, jumlah: d.jumlah || 0, nilai: p.nilai, catatan: p.catatan || '' };
}

async function rute(req, env) {
  const url = new URL(req.url), p = url.pathname.replace(/\/+$/, '') || '/';
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });
  if (req.method === 'GET' && p === '/') return json({ nama: 'taktik-ujian', versi: '2.4', ok: true, db: !!env.DB, api: !!env.API });
  if (!env.DB) return gagal(503, 'Database ujian belum dipasang (binding D1 bernama DB).');
  const db = env.DB;
  await siapkan(db);
  const t = sekarang();

  /* ---------- siswa ---------- */
  if (req.method === 'GET' && p === '/info') {
    const room = normRoom(url.searchParams.get('room'));
    const u = await db.prepare('SELECT room, judul, kelas, aturan, soal, status FROM ujian WHERE room = ?').bind(room).first();
    if (!u) return gagal(404, 'Kode ujian tidak ditemukan. Periksa lagi ke gurumu.');
    const a = JSON.parse(u.aturan || '{}');
    return json({ room: u.room, judul: u.judul, kelas: u.kelas, menit: a.menit, jumlah: JSON.parse(u.soal || '[]').length, status: u.status });
  }
  if (req.method !== 'POST') return gagal(404, 'Alamat tidak dikenal.');
  let b;
  try { b = await req.json(); } catch (e) { return gagal(400, 'Data tidak bisa dibaca.'); }
  if (!b || typeof b !== 'object') return gagal(400, 'Data tidak bisa dibaca.');

  if (p === '/gabung') {
    const room = normRoom(b.room), nama = String(b.nama || '').trim().replace(/\s+/g, ' ').slice(0, 60);
    if (nama.length < 2) return gagal(400, 'Tulis namamu dulu.');
    const u = await db.prepare('SELECT * FROM ujian WHERE room = ?').bind(room).first();
    if (!u) return gagal(404, 'Kode ujian tidak ditemukan. Periksa lagi ke gurumu.');
    const a = JSON.parse(u.aturan || '{}');
    // melanjutkan di HP yang sama
    if (b.pid && b.token) {
      const lama = await db.prepare('SELECT * FROM peserta WHERE id = ? AND room = ? AND token = ?').bind(b.pid, room, b.token).first();
      if (lama) return json({ pid: lama.id, token: lama.token, mulai: lama.mulai, batas: lama.batas, sekarang: t, judul: u.judul, aturan: a, soal: JSON.parse(u.soal), data: JSON.parse(lama.data || '{}'), selesai: !!lama.selesai, status: u.status });
    }
    if (u.status !== 'buka') return gagal(403, 'Ujian ini sudah ditutup oleh guru.');
    const sama = await db.prepare('SELECT id FROM peserta WHERE room = ? AND lower(nama) = lower(?)').bind(room, nama).first();
    if (sama && a.sekali) return gagal(409, 'Nama “' + nama + '” sudah dipakai di ujian ini. Kalau itu kamu, minta guru menghapus namamu dulu, atau tambahkan nama belakang.');
    const pid = acakKode(10), token = acakKode(16), batas = t + a.menit * 60 * 1000;
    await db.prepare('INSERT INTO peserta (id, room, nama, token, mulai, batas, data, selesai, diubah) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)')
      .bind(pid, room, nama, token, t, batas, '{}', t).run();
    return json({ pid, token, mulai: t, batas, sekarang: t, judul: u.judul, aturan: a, soal: JSON.parse(u.soal), data: {}, selesai: false, status: u.status });
  }
  if (p === '/saya') {
    // siswa melihat nilai dan catatan guru untuk ujian yang pernah diikutinya
    const daftar = (Array.isArray(b.ujian) ? b.ujian : []).slice(0, 30);
    const out = [];
    for (const x of daftar) {
      const ps = await db.prepare('SELECT p.id, p.nilai, p.catatan, p.selesai, p.data, u.judul, u.status FROM peserta p JOIN ujian u ON u.room = p.room WHERE p.id = ? AND p.room = ? AND p.token = ?')
        .bind(String(x.pid || ''), normRoom(x.room), String(x.token || '')).first();
      if (!ps) { out.push({ room: normRoom(x.room), pid: x.pid, hilang: true }); continue; }
      let d = {};
      try { d = JSON.parse(ps.data || '{}'); } catch (e) {}
      out.push({ room: normRoom(x.room), pid: ps.id, judul: ps.judul, status: ps.status, nilai: ps.nilai, catatan: ps.catatan || '', skor: d.skor == null ? null : d.skor, benar: d.benar || 0, jumlah: d.jumlah || 0 });
    }
    return json({ ujian: out });
  }
  if (p === '/jawab') {
    const room = normRoom(b.room);
    const ps = await db.prepare('SELECT * FROM peserta WHERE id = ? AND room = ? AND token = ?').bind(b.pid, room, b.token).first();
    if (!ps) return gagal(404, 'Data ujianmu tidak ditemukan.');
    const u = await db.prepare('SELECT status, ditutup FROM ujian WHERE room = ?').bind(room).first();
    const status = u ? u.status : 'tutup';
    if (ps.selesai) return json({ ok: true, selesai: true, status });
    const data = JSON.stringify(b.data || {});
    if (data.length > 200000) return gagal(400, 'Data jawaban terlalu besar.');
    // batas = waktu siswa habis atau saat guru menutup ujian, ditambah masa tenggang untuk HP yang lambat
    const akhirWaktu = Math.min(ps.batas, u && u.status === 'tutup' && u.ditutup ? u.ditutup : Infinity) + TENGGANG;
    if (t <= akhirWaktu) {
      await db.prepare('UPDATE peserta SET data = ?, selesai = ?, diubah = ? WHERE id = ?').bind(data, b.akhir ? 1 : 0, t, ps.id).run();
      return json({ ok: true, selesai: !!b.akhir, status, sekarang: t });
    }
    // terlambat: lembar kerja yang terakhir tersimpan yang dipakai
    if (b.akhir) await db.prepare('UPDATE peserta SET selesai = 1 WHERE id = ?').bind(ps.id).run();
    return json({ ok: false, waktuHabis: true, selesai: !!b.akhir, status, sekarang: t });
  }

  /* ---------- guru ---------- */
  const kode = normKode(b.kode);
  const tolak = await cekGuru(env, kode, b.pin, req);
  if (tolak) return tolak;
  const milik = async room => {
    const u = await db.prepare('SELECT * FROM ujian WHERE room = ? AND kode = ?').bind(normRoom(room), kode).first();
    return u || null;
  };

  if (p === '/buat') {
    const soal = Array.isArray(b.soal) ? b.soal.slice(0, MAX_SOAL) : [];
    if (!soal.length) return gagal(400, 'Pilih minimal satu soal.');
    const s = JSON.stringify(soal);
    if (s.length > MAX_BYTES) return gagal(400, 'Soal terlalu besar.');
    let room;
    for (let i = 0; i < 5; i++) {
      room = acakKode(6);
      if (!(await db.prepare('SELECT room FROM ujian WHERE room = ?').bind(room).first())) break;
    }
    await db.prepare('INSERT INTO ujian (room, kode, judul, kelas, aturan, soal, status, dibuat) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
      .bind(room, kode, String(b.judul || 'Ujian').slice(0, 80), String(b.kelas || ''), JSON.stringify(aturanBersih(b.aturan)), s, 'buka', t).run();
    return json({ room });
  }
  if (p === '/daftar') {
    const r = await db.prepare(`SELECT u.room, u.judul, u.kelas, u.aturan, u.status, u.dibuat,
        (SELECT COUNT(*) FROM peserta p WHERE p.room = u.room) AS peserta,
        (SELECT COUNT(*) FROM peserta p WHERE p.room = u.room AND p.selesai = 1) AS selesai
      FROM ujian u WHERE u.kode = ? ORDER BY u.dibuat DESC LIMIT 50`).bind(kode).all();
    return json({ ujian: (r.results || []).map(x => ({ room: x.room, judul: x.judul, kelas: x.kelas, menit: JSON.parse(x.aturan || '{}').menit, status: x.status, dibuat: x.dibuat, peserta: x.peserta, selesai: x.selesai })) });
  }
  if (p === '/hasil') {
    const u = await milik(b.room);
    if (!u) return gagal(404, 'Ujian tidak ditemukan.');
    const r = await db.prepare('SELECT * FROM peserta WHERE room = ? ORDER BY mulai').bind(u.room).all();
    return json({ room: u.room, judul: u.judul, kelas: u.kelas, aturan: JSON.parse(u.aturan || '{}'), status: u.status, jumlahSoal: JSON.parse(u.soal || '[]').length,
      sekarang: t, peserta: (r.results || []).map(x => {
        const o = ringkas(x, u, t);
        // b.detail: sertakan jawaban per soal untuk analisis soal dan berkas nilai
        if (b.detail) { try { o.res = (JSON.parse(x.data || '{}').res || []).map(y => ({ id: y.id, judul: y.judul, topik: y.topik, ok: !!y.ok, err: y.err || 0, lewat: !!y.lewat })); } catch (e) { o.res = []; } }
        return o;
      }) });
  }
  if (p === '/lembar') {
    const u = await milik(b.room);
    if (!u) return gagal(404, 'Ujian tidak ditemukan.');
    const x = await db.prepare('SELECT * FROM peserta WHERE id = ? AND room = ?').bind(b.pid, u.room).first();
    if (!x) return gagal(404, 'Peserta tidak ditemukan.');
    return json({ ...ringkas(x, u, t), data: JSON.parse(x.data || '{}') });
  }
  if (p === '/nilai') {
    const u = await milik(b.room);
    if (!u) return gagal(404, 'Ujian tidak ditemukan.');
    const n = b.nilai === '' || b.nilai == null ? null : Math.max(0, Math.min(100, +b.nilai));
    if (n != null && !Number.isFinite(n)) return gagal(400, 'Nilai harus angka 0–100.');
    await db.prepare('UPDATE peserta SET nilai = ?, catatan = ? WHERE id = ? AND room = ?').bind(n, String(b.catatan || '').slice(0, 500), b.pid, u.room).run();
    return json({ ok: true });
  }
  if (p === '/hapus-peserta') {
    const u = await milik(b.room);
    if (!u) return gagal(404, 'Ujian tidak ditemukan.');
    await db.prepare('DELETE FROM peserta WHERE id = ? AND room = ?').bind(b.pid, u.room).run();
    return json({ ok: true });
  }
  if (p === '/status') {
    const u = await milik(b.room);
    if (!u) return gagal(404, 'Ujian tidak ditemukan.');
    const st = b.status === 'buka' ? 'buka' : 'tutup';
    await db.prepare('UPDATE ujian SET status = ?, ditutup = ? WHERE room = ?').bind(st, st === 'tutup' ? t : null, u.room).run();
    return json({ ok: true, status: st });
  }
  if (p === '/hapus') {
    const u = await milik(b.room);
    if (!u) return gagal(404, 'Ujian tidak ditemukan.');
    await db.batch([db.prepare('DELETE FROM peserta WHERE room = ?').bind(u.room), db.prepare('DELETE FROM ujian WHERE room = ?').bind(u.room)]);
    return json({ ok: true });
  }
  return gagal(404, 'Alamat tidak dikenal.');
}

export default {
  async fetch(req, env) {
    try { return await rute(req, env); }
    catch (e) { return gagal(500, 'Server ujian bermasalah. Coba lagi.'); }
  }
};
