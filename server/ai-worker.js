// TakTik IPA — server AI pembuat soal (Cloudflare Worker, terpisah dari server soal).
// Server soal yang sudah berjalan tidak diubah: PIN guru dicek ke server itu lewat /login.
// Kode ini berupa satu berkas supaya bisa ditempel langsung di dasbor Cloudflare (tanpa bundler),
// karena itu Claude dipanggil lewat HTTP biasa.
//
// Pengaturan (Settings → Variables and Secrets):
//   ANTHROPIC_API_KEY  secret, kunci API dari console.anthropic.com (wajib untuk memakai Claude)
//   CLAUDE_MODEL       opsional, bawaan claude-opus-5-5
//   MAIN_API           opsional, alamat server soal (bawaan https://taktik-api.taktik-ipa.workers.dev)
//   AI_HARIAN          opsional, batas permintaan per kode kelas per hari (bawaan 40)
// Binding opsional: KV "DB" untuk menghitung kuota harian, Workers AI "AI" sebagai cadangan kalau
// ANTHROPIC_API_KEY belum diisi.

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

const SISTEM = `Kamu membantu guru IPA SMP di Indonesia (Kurikulum Merdeka) membuat soal cerita hitungan untuk aplikasi TakTik IPA.
Siswa memilah angka di cerita ke kotak besaran, mencoret angka pengecoh, mengenali materi, memilih rumus, lalu menghitung.
Kalau ada templat yang cocok, aplikasi yang merakit rumus, pilihan jawaban, dan menghitung jawabannya; tugasmu memilih templat, angka, dan menulis cerita yang bagus, dan "lengkap" diisi string kosong.
Kalau tidak ada templat yang cocok, isi "templat" dengan string kosong dan tulis SOAL LENGKAP sebagai teks JSON di "lengkap" (format ada di pesan guru). Hitung jawabannya dengan teliti.
Ikuti aturan dan DAFTAR TEMPLAT di pesan guru. Pakai bahasa Indonesia yang wajar untuk siswa SMP dan konteks sehari-hari di Indonesia.
"pesan" berisi satu atau dua kalimat singkat untuk guru. Kalau guru meminta perbaikan, kirim ulang soal yang sudah diperbaiki.`;

// Struktur jawaban yang dipaksa lewat structured outputs.
const SKEMA = {
  type: 'object',
  properties: {
    pesan: { type: 'string' },
    soal: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          templat: { type: 'string' },
          // soal lengkap dalam teks JSON, hanya kalau tidak ada templat yang cocok
          lengkap: { type: 'string' },
          judul: { type: 'string' },
          cerita: { type: 'string' },
          acak: { type: 'boolean' },
          nilai: {
            type: 'array',
            items: {
              type: 'object',
              properties: { k: { type: 'string' }, v: { type: 'number' } },
              required: ['k', 'v'],
              additionalProperties: false
            }
          }
        },
        required: ['templat', 'lengkap', 'judul', 'cerita', 'acak', 'nilai'],
        additionalProperties: false
      }
    }
  },
  required: ['pesan', 'soal'],
  additionalProperties: false
};

function riwayatDari(pesan) {
  const r = (Array.isArray(pesan) ? pesan : []).slice(-8)
    .filter(m => m && (m.role === 'user' || m.role === 'assistant') && typeof m.content === 'string' && m.content.trim())
    .map(m => ({ role: m.role, content: m.content.slice(0, 20000) }));
  while (r.length && r[0].role !== 'user') r.shift();
  return r;
}

/* Claude: jawaban berupa JSON yang dijamin sesuai SKEMA */
async function tanyaClaude(env, riwayat) {
  const model = env.CLAUDE_MODEL || 'claude-opus-5-5';
  const headers = {
    'Content-Type': 'application/json',
    'x-api-key': env.ANTHROPIC_API_KEY,
    'anthropic-version': '2026-10-21'
  };
  const body = {
    model,
    max_tokens: 16000,
    system: SISTEM,
    messages: riwayat,
    output_config: { effort: 'medium', format: { type: 'json_schema', schema: SKEMA } }
  };
  // Kalau permintaan ditolak pengaman model, server Anthropic mengulang dengan model cadangan.
  if (/^claude-(opus-5|fable-5|sonnet-5-5)/.test(model)) {
    headers['anthropic-beta'] = 'server-side-fallback-2026-07-01';
    body.fallbacks = 'default';
  }
  const r = await fetch('https://api.anthropic.com/v1/messages', { method: 'POST', headers, body: JSON.stringify(body) });
  const j = await r.json().catch(() => null);
  if (!r.ok || !j) {
    const tipe = j && j.error && j.error.type;
    if (r.status === 401) throw new Error('Kunci API Claude tidak valid. Periksa ANTHROPIC_API_KEY.');
    if (r.status === 429 || tipe === 'rate_limit_error') throw new Error('AI sedang sibuk. Coba lagi sebentar lagi.');
    if (tipe === 'billing_error' || r.status === 402) throw new Error('Saldo API Claude habis. Isi ulang di console.anthropic.com.');
    throw new Error('AI belum bisa menjawab (' + r.status + '). Coba lagi.');
  }
  if (j.stop_reason === 'refusal') throw new Error('AI menolak permintaan ini. Coba ubah kalimat permintaannya.');
  if (j.stop_reason === 'max_tokens') throw new Error('Jawaban AI terpotong. Minta lebih sedikit soal sekaligus.');
  const teks = (j.content || []).filter(b => b.type === 'text').map(b => b.text).join('');
  let o;
  try { o = JSON.parse(teks); } catch (e) { throw new Error('Jawaban AI tidak bisa dibaca. Coba lagi.'); }
  // Format yang sama dengan server lama: kalimat untuk guru + satu blok ```json.
  const soal = (o.soal || []).slice(0, 3).map(x => {
    if (x.templat || !x.lengkap) { const { lengkap, ...t } = x; return t; }
    try { return { templat: '', lengkap: JSON.parse(x.lengkap) }; } catch (e) { return { templat: '', lengkap: null, judul: x.judul, cerita: x.cerita }; }
  });
  return String(o.pesan || 'Ini soalnya.') + '\n```json\n' + JSON.stringify({ soal }) + '\n```';
}

/* Cadangan tanpa kunci Claude: Workers AI gratis (kualitas lebih rendah) */
async function tanyaWorkersAI(env, riwayat) {
  if (!env.AI) throw new Error('AI belum dipasang. Isi ANTHROPIC_API_KEY di pengaturan server AI.');
  const out = await env.AI.run('@cf/meta/llama-3.3-70b-instruct-fp8-fast', {
    messages: [{ role: 'system', content: SISTEM }, ...riwayat], max_tokens: 4096
  });
  const b = out && (out.response || (out.result && out.result.response));
  if (!b) throw new Error('AI tidak memberi jawaban. Coba lagi.');
  return String(b);
}

async function cekGuru(env, kode, pin) {
  const api = (env.MAIN_API || 'https://taktik-api.taktik-ipa.workers.dev').replace(/\/+$/, '');
  try {
    const r = await fetch(api + '/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ kode, pin }) });
    if (r.ok) return null;
    const j = await r.json().catch(() => null);
    return gagal(r.status === 429 ? 429 : 401, (j && j.pesan) || 'PIN salah. Masuk lagi dengan PIN terbaru.');
  } catch (e) {
    return gagal(502, 'Server soal tidak bisa dihubungi. Coba lagi.');
  }
}

async function ai(req, env) {
  let b;
  try { b = await req.json(); } catch (e) { return gagal(400, 'Data tidak bisa dibaca.'); }
  const kode = String((b && b.kode) || 'UTAMA').trim().toUpperCase();
  if (!/^[A-Z0-9-]{3,20}$/.test(kode)) return gagal(400, 'Kode kelas tidak valid.');
  const tolak = await cekGuru(env, kode, b.pin);
  if (tolak) return tolak;
  const riwayat = riwayatDari(b.pesan);
  if (!riwayat.length || riwayat[riwayat.length - 1].role !== 'user') return gagal(400, 'Pesan kosong.');

  const batas = +env.AI_HARIAN || 40;
  const tgl = new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10);
  const kunci = 'ai:' + kode + ':' + tgl;
  const n = env.DB ? (+(await env.DB.get(kunci)) || 0) : 0;
  if (n >= batas) return gagal(429, 'Kuota AI hari ini sudah habis (' + batas + ' permintaan). Coba lagi besok.');

  let balasan;
  try {
    balasan = env.ANTHROPIC_API_KEY ? await tanyaClaude(env, riwayat) : await tanyaWorkersAI(env, riwayat);
  } catch (e) {
    return gagal(502, e.message || 'AI belum bisa menjawab. Coba lagi.');
  }
  if (env.DB) await env.DB.put(kunci, String(n + 1), { expirationTtl: 2 * 86400 });
  return json({ balasan, sisa: env.DB ? batas - n - 1 : null });
}

export default {
  async fetch(req, env) {
    const p = new URL(req.url).pathname.replace(/\/+$/, '') || '/';
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });
    if (req.method === 'GET' && p === '/') return json({ nama: 'taktik-ai', ok: true, claude: !!env.ANTHROPIC_API_KEY });
    if (req.method === 'POST' && p === '/ai') {
      try { return await ai(req, env); } catch (e) { return gagal(500, 'Server AI bermasalah. Coba lagi.'); }
    }
    return gagal(404, 'Alamat tidak dikenal.');
  }
};
