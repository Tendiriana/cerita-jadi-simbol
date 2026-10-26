package id.taktik.xmod

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Grafik garis lalu lintas jaringan yang bergeser tiap tick. */
class SparklineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val n = 22
    private val vals = FloatArray(n) { 0.3f + 0.4f * Math.random().toFloat() }

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE8B24A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1.4f)
        strokeJoin = Paint.Join.ROUND
    }
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2E2A1C.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(dp(2f), dp(4f)), 0f)
    }
    private val path = Path()

    private fun dp(v: Float) = v * resources.displayMetrics.density

    /** Dorong nilai baru (0..1), geser ke kiri. */
    fun push(v: Float) {
        for (i in 0 until n - 1) vals[i] = vals[i + 1]
        vals[n - 1] = v.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = dp(3f)
        canvas.drawLine(0f, h / 2, w, h / 2, base)
        canvas.drawLine(0f, h - 1, w, h - 1, base)
        path.reset()
        val step = w / (n - 1)
        for (i in 0 until n) {
            val x = i * step
            val y = pad + (1f - vals[i]) * (h - pad * 2)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, line)
    }
}
