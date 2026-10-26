package id.taktik.xmod

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Random

/** Kilatan glitch singkat saat transisi. */
class GlitchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var tint = 0xFFE8B24A.toInt()
    private var frames = 0
    private val paint = Paint()
    private val rnd = Random()

    fun trigger() { frames = 5; invalidate() }

    override fun onDraw(canvas: Canvas) {
        if (frames <= 0) return
        val w = width.toFloat(); val h = height
        for (i in 0 until 7) {
            val y = rnd.nextInt(h)
            val sh = 18 + rnd.nextInt(56)
            paint.color = (0x30 shl 24) or (tint and 0x00FFFFFF)
            canvas.drawRect(0f, y.toFloat(), w, (y + sh).toFloat(), paint)
        }
        paint.color = 0x3000E5FF
        val cy = rnd.nextInt(h).toFloat()
        canvas.drawRect(0f, cy, w * 0.6f, cy + 5f, paint)
        frames--
        if (frames > 0) postInvalidateDelayed(32)
    }
}
