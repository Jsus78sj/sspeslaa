package com.speedmeter.lite.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

/**
 * كل التواريخ تُحسب بالتوقيت المحلي وبالتقويم الميلادي وبأرقام لاتينية،
 * حتى لا تتأثر مفاتيح قاعدة البيانات بلغة الجهاز (مثلاً الأرقام العربية الهندية).
 */
object DateUtils {

    /** بداية اليوم المحلي (منتصف الليل المحلي) بالميلي ثانية. */
    fun startOfToday(): Long = GregorianCalendar().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** مفتاح اليوم بصيغة yyyy-MM-dd، مع إمكانية الرجوع عدداً من الأيام. */
    fun dayKey(daysAgo: Int = 0): String {
        val cal = GregorianCalendar().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    /** رقم اليوم في الشهر (للتسمية تحت الأعمدة). */
    fun dayOfMonth(daysAgo: Int): String {
        val cal = GregorianCalendar().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        return cal.get(Calendar.DAY_OF_MONTH).toString()
    }
}
