package id.taktik.xmod

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Empat bracket sudut ala HUD di atas foto. */
class HudBracketsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE8B24A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val inset = dp(14f)
    private val topPad = dp(52f)
    private val botPad = dp(150f)
    private val len = dp(16f)

    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        val l = inset
        val r = width - inset
        val t = topPad
        val b = height - botPad
        // kiri atas
        canvas.drawLine(l, t, l + len, t, paint)
        canvas.drawLine(l, t, l, t + len, paint)
        // kanan atas
        canvas.drawLine(r, t, r - len, t, paint)
        canvas.drawLine(r, t, r, t + len, paint)
        // kiri bawah
        canvas.drawLine(l, b, l + len, b, paint)
        canvas.drawLine(l, b, l, b - len, paint)
        // kanan bawah
        canvas.drawLine(r, b, r - len, b, paint)
        canvas.drawLine(r, b, r, b - len, paint)
    }
}
