// Service worker TakTik IPA.
// Jaringan dulu (selalu cek versi terbaru), cadangan dari cache supaya tetap terbuka saat offline.
// Setiap kali aplikasi diperbarui, naikkan VERSI di sini dan APP_VERSI di index.html.
// HP yang sudah memasang aplikasi otomatis mendapat versi baru, tanpa perlu uninstall.
const VERSI = '2.12.0';
const C = 'taktik-' + VERSI;
const INTI = ['./', 'index.html', 'soal.json', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png', 'icon-512-maskable.png', 'apple-touch-icon.png',
  'fonts/kalam-400.woff2', 'fonts/kalam-700.woff2', 'fonts/andika-400.woff2', 'fonts/andika-700.woff2',
  'fonts/gochi-hand.woff2', 'fonts/source-serif-4.woff2', 'fonts/plex-mono-500.woff2', 'fonts/plex-mono-600.woff2'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(C)
    .then(c => c.addAll(INTI.map(u => new Request(u, { cache: 'reload' }))))
    .then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(ks => Promise.all(ks.filter(k => k !== C).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const req = e.request, u = new URL(req.url);
  if (req.method !== 'GET' || u.origin !== location.origin) return;
  const nav = req.mode === 'navigate';
  // Halaman dan soal selalu diambil segar: alamat diberi penanda waktu supaya salinan di CDN GitHub
  // (bisa sampai 10 menit) dilewati. Berkas lain tetap memakai cache HTTP biasa.
  const segar = nav || /\/(index\.html|soal\.json)?$/.test(u.pathname);
  let ambil;
  if (segar) {
    const b = new URL(u.href);
    b.searchParams.set('_t', Date.now());
    ambil = fetch(b.href, { cache: 'no-store', credentials: 'same-origin' });
  } else ambil = fetch(req, { cache: 'no-cache' });
  e.respondWith(
    ambil.then(r => {
      if (r && r.ok && r.type === 'basic') {
        const copy = r.clone();
        caches.open(C).then(c => c.put(nav ? 'index.html' : (segar ? u.origin + u.pathname : req), copy));
      }
      return r;
    }).catch(() => caches.match(nav ? 'index.html' : req, { ignoreSearch: true })
      .then(m => m || (nav ? caches.match('./') : undefined))
      .then(m => m || new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/plain; charset=utf-8' } })))
  );
});
