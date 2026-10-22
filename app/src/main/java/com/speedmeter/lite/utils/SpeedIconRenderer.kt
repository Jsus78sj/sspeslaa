package com.speedmeter.lite.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale

/**
 * يرسم السرعة (الرقم فوق والوحدة تحته) داخل أيقونة صغيرة لتظهر في شريط الحالة،
 * مثل: 325 / kB/s
 *
 * أيقونات شريط الحالة يلوّنها النظام بنفسه، لذلك نرسم بالأبيض فقط على خلفية شفافة.
 */
object SpeedIconRenderer {
    private const val SIZE = 96          // بكسل؛ النظام يصغّرها لحجم أيقونة شريط الحالة
    private const val NUMBER_MAX_H = 52f // أقصى ارتفاع للرقم
    private const val UNIT_MAX_H = 30f   // أقصى ارتفاع للوحدة
    private const val GAP = 6f           // المسافة بين الرقم والوحدة

    private const val KB = 1024L
    private const val MB = 1024L * 1024

    private val condensedBold: Typeface =
        Typeface.create("sans-serif-condensed", Typeface.BOLD)

    /** صيغة مختصرة تناسب المساحة الضيقة: 0 B/s ، 325 kB/s ، 1.5 MB/s ، 12 MB/s */
    fun parts(bytesPerSec: Long): Pair<String, String> = when {
        bytesPerSec < KB -> Pair("$bytesPerSec", "B/s")
        bytesPerSec < MB -> Pair("${bytesPerSec / KB}", "kB/s")
        else -> {
            val mb = bytesPerSec / MB.toDouble()
            val text = if (mb < 10) String.format(Locale.US, "%.1f", mb)
            else String.format(Locale.US, "%.0f", mb)
            Pair(text, "MB/s")
        }
    }

    fun icon(bytesPerSec: Long): IconCompat {
        val (value, unit) = parts(bytesPerSec)
        return IconCompat.createWithBitmap(render(value, unit))
    }

    private fun render(value: String, unit: String): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val numPaint = newPaint()
        val unitPaint = newPaint()
        fit(numPaint, value, SIZE.toFloat(), NUMBER_MAX_H)
        fit(unitPaint, unit, SIZE.toFloat(), UNIT_MAX_H)

        val nb = Rect().also { numPaint.getTextBounds(value, 0, value.length, it) }
        val ub = Rect().also { unitPaint.getTextBounds(unit, 0, unit.length, it) }

        // نوسّط الكتلة (الرقم + الوحدة) عمودياً
        val total = nb.height() + GAP + ub.height()
        val top = (SIZE - total) / 2f
        val numBaseline = top - nb.top                    // nb.top سالب (فوق خط الأساس)
        val unitBaseline = top + nb.height() + GAP + ub.height() - ub.bottom

        val cx = SIZE / 2f
        canvas.drawText(value, cx, numBaseline, numPaint)
        canvas.drawText(unit, cx, unitBaseline, unitPaint)
        return bmp
    }

    private fun newPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = condensedBold
        textAlign = Paint.Align.CENTER
    }

    /** يضبط حجم الخط ليتّسع النص داخل العرض والارتفاع المحدّدين. */
    private fun fit(p: Paint, text: String, maxW: Float, maxH: Float) {
        p.textSize = 100f
        val r = Rect()
        p.getTextBounds(text, 0, text.length, r)
        val scale = minOf(maxW / r.width().coerceAtLeast(1), maxH / r.height().coerceAtLeast(1))
        p.textSize = 100f * scale
    }
}
