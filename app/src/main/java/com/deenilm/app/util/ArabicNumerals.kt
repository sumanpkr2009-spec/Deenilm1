package com.deenilm.app.util

private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun Int.toArabicIndic(): String = toString().map { c ->
    if (c in '0'..'9') ARABIC_DIGITS[c - '0'] else c
}.joinToString("")
