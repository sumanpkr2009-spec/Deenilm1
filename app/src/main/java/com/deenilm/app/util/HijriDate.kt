package com.deenilm.app.util

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun hijriDateString(): String = try {
    val h = HijrahDate.from(LocalDate.now())
    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    h.format(fmt) + " AH"
} catch (e: Exception) {
    ""
}
