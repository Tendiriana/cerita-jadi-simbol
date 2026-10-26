package id.taktik.xmod

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import kotlin.math.abs

/**
 * Mengubah ikon aplikasi apa pun menjadi sketsa garis satu warna (amber).
 *
 * Caranya: ikon digambar ke bitmap, lalu tiap piksel dihitung kecerahannya
 * (piksel transparan dianggap latar terang supaya garis tepi siluet muncul),
 * lalu dideteksi tepinya. Tepi yang kuat jadi garis tegas, isi dalamnya diberi
 * sedikit warna samar. Semua ikon jadi seragam tanpa menyentuh aplikasi aslinya.
 */
object IconRenderer {

    fun render(src: Drawable, size: Int, accent: Int): Bitmap {
        val base = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(base)
        src.setBounds(0, 0, size, size)
        src.draw(canvas)

        val n = size * size
        val px = IntArray(n)
        base.getPixels(px, 0, size, 0, 0, size, size)

        // Kecerahan tiap piksel; transparan dianggap terang (235) agar siluet bertepi.
        val lum = FloatArray(n)
        for (i in 0 until n) {
            val a = (px[i] ushr 24) and 0xFF
            if (a < 24) {
                lum[i] = 235f
            } else {
                val r = (px[i] ushr 16) and 0xFF
                val g = (px[i] ushr 8) and 0xFF
                val b = px[i] and 0xFF
                lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
            }
        }

        val ar = Color.red(accent)
        val ag = Color.green(accent)
        val ab = Color.blue(accent)
        val out = IntArray(n)

        for (y in 0 until size) {
            val ym = if (y > 0) y - 1 else y
            val yp = if (y < size - 1) y + 1 else y
            for (x in 0 until size) {
                val i = y * size + x
                val xm = if (x > 0) x - 1 else x
                val xp = if (x < size - 1) x + 1 else x

                val gx = lum[y * size + xp] - lum[y * size + xm]
                val gy = lum[yp * size + x] - lum[ym * size + x]
                val mag = abs(gx) + abs(gy)

                var alpha = (mag * 1.6f).toInt()
                if (alpha > 255) alpha = 255

                // Isi dalam diberi warna samar supaya tidak kosong melompong.
                val oa = (px[i] ushr 24) and 0xFF
                if (oa >= 24 && alpha < 40) alpha = 40

                out[i] = (alpha shl 24) or (ar shl 16) or (ag shl 8) or ab
            }
        }

        base.setPixels(out, 0, size, 0, 0, size, size)
        return base
    }
}
