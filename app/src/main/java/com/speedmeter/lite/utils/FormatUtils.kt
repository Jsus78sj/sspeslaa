package com.speedmeter.lite.utils

import java.util.Locale

object FormatUtils {
    private const val KB = 1024L
    private const val MB = 1024L * 1024
    private const val GB = 1024L * 1024 * 1024

    /** يعيد (القيمة، الوحدة) لسرعة بالبايت/ثانية. */
    fun speedParts(bytesPerSec: Long): Pair<String, String> = when {
        bytesPerSec < KB -> Pair("$bytesPerSec", "B/s")
        bytesPerSec < MB -> Pair(String.format(Locale.US, "%.1f", bytesPerSec / KB.toDouble()), "KB/s")
        else -> Pair(String.format(Locale.US, "%.2f", bytesPerSec / MB.toDouble()), "MB/s")
    }

    fun speed(bytesPerSec: Long): String {
        val (v, u) = speedParts(bytesPerSec)
        return "$v $u"
    }

    fun bytes(b: Long): String = when {
        b < KB -> "$b B"
        b < MB -> String.format(Locale.US, "%.1f KB", b / KB.toDouble())
        b < GB -> String.format(Locale.US, "%.1f MB", b / MB.toDouble())
        else -> String.format(Locale.US, "%.2f GB", b / GB.toDouble())
    }
}
