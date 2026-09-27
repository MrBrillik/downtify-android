package com.henriquesebastiao.downtify.core.model

import java.util.Locale

/** `3:41`, or `1:02:09` for an hour or more. */
fun formatDuration(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h >
        0
    ) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
}

/** Minutes, rounded, for an album total ("41 min"). */
fun totalMinutes(seconds: Double): Int = ((seconds + 30) / 60).toInt()

/** `FLAC`, `MP3`, … for a codec name. */
fun codecLabel(codec: String): String = codec.uppercase(Locale.ROOT)
