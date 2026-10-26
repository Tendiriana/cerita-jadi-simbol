package id.taktik.xmod

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Random

/** Hujan kode samar ala Matrix di latar belakang. */
class MatrixRainView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var rainColor = 0xFFE8B24A.toInt()
        set(value) { field = value; invalidate() }

    private val glyphs = "01xmod</>=+*#[]{}".toCharArray()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val rnd = Random()
    private var cell = 0f
    private var cols = 0
    private var rows = 0
    private var head = IntArray(0)
    private var started = false

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        cell = resources.displayMetrics.density * 13f
        paint.textSize = cell * 0.85f
        cols = (w / cell).toInt().coerceAtLeast(1)
        rows = (h / cell).toInt().coerceAtLeast(1)
        head = IntArray(cols) { rnd.nextInt(rows + 1) }
        if (!started) { started = true; invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        if (cell <= 0f || head.isEmpty()) return
        val trail = 6
        for (x in 0 until cols) {
            val hy = head[x]
            for (t in 0 until trail) {
                val y = hy - t
                if (y < 0) continue
                val ch = glyphs[(x * 7 + y * 13 + t) % glyphs.size]
                val a = when (t) { 0 -> 120; 1 -> 70; else -> (52 - t * 8).coerceAtLeast(10) }
                paint.color = (rainColor and 0x00FFFFFF) or (a shl 24)
                canvas.drawText(ch.toString(), x * cell + cell * 0.1f, y * cell + cell * 0.85f, paint)
            }
            head[x] = hy + 1
            if (head[x] > rows + trail + rnd.nextInt(rows + 1)) head[x] = 0
        }
        postInvalidateDelayed(95)
    }
}
