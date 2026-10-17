// Service worker: jaringan dulu, cadangan dari cache supaya aplikasi tetap terbuka saat offline.
const C = 'cjs-v3';
self.addEventListener('install', e => {
  e.waitUntil(caches.open(C).then(c => c.addAll(['./', 'index.html', 'soal.json', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'])).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== C).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;
  e.respondWith(
    fetch(e.request).then(r => {
      const copy = r.clone();
      caches.open(C).then(c => c.put(e.request, copy));
      return r;
    }).catch(() => caches.match(e.request))
  );
});
