// Service worker TakTik IPA.
// Jaringan dulu (selalu cek versi terbaru), cadangan dari cache supaya tetap terbuka saat offline.
// Setiap kali aplikasi diperbarui, naikkan VERSI di sini dan APP_VERSI di index.html.
// HP yang sudah memasang aplikasi otomatis mendapat versi baru, tanpa perlu uninstall.
const VERSI = '2.4.0';
const C = 'taktik-' + VERSI;
const INTI = ['./', 'index.html', 'soal.json', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png', 'icon-512-maskable.png', 'apple-touch-icon.png'];

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
  e.respondWith(
    // no-cache: tetap memakai cache HTTP, tetapi selalu bertanya ke server apakah ada versi baru
    fetch(req, { cache: 'no-cache' }).then(r => {
      if (r && r.ok && r.type === 'basic') {
        const copy = r.clone();
        caches.open(C).then(c => c.put(nav ? 'index.html' : req, copy));
      }
      return r;
    }).catch(() => caches.match(nav ? 'index.html' : req, { ignoreSearch: true })
      .then(m => m || (nav ? caches.match('./') : undefined))
      .then(m => m || new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/plain; charset=utf-8' } })))
  );
});
