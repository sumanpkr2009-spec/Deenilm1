package com.deenilm.app.data

import java.text.Normalizer

/**
 * Roman → Indic/Urdu pronunciation converter, ported from the Deen Ilm web app.
 * Converts the Latin transliteration of Arabic (e.g. from alquran.cloud's
 * en.transliteration edition) into Devanagari (Hindi), Bangla, or Urdu script
 * as a reading aid. This does NOT change the canonical Arabic text — it is
 * only a pronunciation helper, and the UI must label it as such.
 *
 * script: "en" = Latin/Roman (normalized), "hi" = Devanagari,
 *         "bn" = Bangla, "ur" = Urdu.
 */
object Pronunciation {

    private data class Token(val kind: String, val value: String) // kind: vowel|consonant

    private val consonants = listOf(
        "sh", "kh", "gh", "th", "dh", "ch", "ph", "bh", "zh",
        "q", "b", "t", "d", "j", "h", "f", "k", "g", "l", "m",
        "n", "r", "s", "z", "w", "y", "c", "p", "v", "x"
    )
    private val vowels = listOf(
        "aa", "ee", "ii", "oo", "uu", "ai", "ay", "au", "aw",
        "a", "i", "u", "e", "o"
    )
    private val joinedConsonants = setOf(
        "sm", "st", "sk", "sp", "nd", "nt", "rt", "rk", "rm", "rn",
        "rd", "rb", "rj", "rq", "rs", "lm", "ln", "lk", "lb", "ld",
        "lf", "lq", "kt", "kr", "kl", "mn", "mb", "ms", "nh", "ll",
        "mm", "nn", "rr", "ss", "tt", "dd"
    )

    private val hiConsonants = mapOf(
        "sh" to "श", "kh" to "ख़", "gh" to "ग़", "th" to "थ", "dh" to "ध",
        "ch" to "च", "ph" to "फ़", "bh" to "भ", "zh" to "ज़", "q" to "क़",
        "b" to "ब", "t" to "त", "d" to "द", "j" to "ज", "h" to "ह",
        "f" to "फ़", "k" to "क", "g" to "ग", "l" to "ल", "m" to "म",
        "n" to "न", "r" to "र", "s" to "स", "z" to "ज़", "w" to "व",
        "y" to "य", "c" to "क", "p" to "प", "v" to "व", "x" to "क्स"
    )
    private val hiVowels = mapOf(
        "aa" to listOf("आ", "ा"), "ee" to listOf("ई", "ी"), "ii" to listOf("ई", "ी"),
        "oo" to listOf("ऊ", "ू"), "uu" to listOf("ऊ", "ू"), "ai" to listOf("ऐ", "ै"),
        "ay" to listOf("ऐ", "ै"), "au" to listOf("औ", "ौ"), "aw" to listOf("औ", "ौ"),
        "a" to listOf("अ", ""), "i" to listOf("इ", "ि"), "u" to listOf("उ", "ु"),
        "e" to listOf("ए", "े"), "o" to listOf("ओ", "ो")
    )
    private const val HI_VIRAMA = "्"

    private val bnConsonants = mapOf(
        "sh" to "শ", "kh" to "খ", "gh" to "ঘ", "th" to "থ", "dh" to "ধ",
        "ch" to "চ", "ph" to "ফ", "bh" to "ভ", "zh" to "জ", "q" to "ক",
        "b" to "ব", "t" to "ত", "d" to "দ", "j" to "জ", "h" to "হ",
        "f" to "ফ", "k" to "ক", "g" to "গ", "l" to "ল", "m" to "ম",
        "n" to "ন", "r" to "র", "s" to "স", "z" to "জ", "w" to "ওয়",
        "y" to "ইয়", "c" to "ক", "p" to "প", "v" to "ভ", "x" to "ক্স"
    )
    private val bnVowels = mapOf(
        "aa" to listOf("আ", "া"), "ee" to listOf("ঈ", "ী"), "ii" to listOf("ঈ", "ী"),
        "oo" to listOf("ঊ", "ূ"), "uu" to listOf("ঊ", "ূ"), "ai" to listOf("আই", "াই"),
        "ay" to listOf("এই", "েই"), "au" to listOf("আউ", "াউ"), "aw" to listOf("আও", "াও"),
        "a" to listOf("আ", "া"), "i" to listOf("ই", "ি"), "u" to listOf("উ", "ু"),
        "e" to listOf("এ", "ে"), "o" to listOf("ও", "ো")
    )
    private const val BN_VIRAMA = "্"

    private val urConsonants = mapOf(
        "sh" to "ش", "kh" to "خ", "gh" to "غ", "th" to "ث", "dh" to "ذ",
        "ch" to "چ", "ph" to "ف", "bh" to "بھ", "zh" to "ژ", "q" to "ق",
        "b" to "ب", "t" to "ت", "d" to "د", "j" to "ج", "h" to "ہ",
        "f" to "ف", "k" to "ک", "g" to "گ", "l" to "ل", "m" to "م",
        "n" to "ن", "r" to "ر", "s" to "س", "z" to "ز", "w" to "و",
        "y" to "ی", "c" to "ک", "p" to "پ", "v" to "و", "x" to "کس"
    )
    private val urAttachedVowels = mapOf(
        "aa" to "ا", "ee" to "ی", "ii" to "ی", "oo" to "و", "uu" to "و",
        "ai" to "َی", "ay" to "َی", "au" to "َو", "aw" to "َو",
        "a" to "َ", "i" to "ِ", "u" to "ُ", "e" to "ے", "o" to "و"
    )
    private val urInitialVowels = mapOf(
        "aa" to "آ", "ee" to "ای", "ii" to "ای", "oo" to "او", "uu" to "او",
        "ai" to "ای", "ay" to "ای", "au" to "او", "aw" to "او",
        "a" to "اَ", "i" to "اِ", "u" to "اُ", "e" to "اے", "o" to "او"
    )

    private fun tokenize(word: String): List<Token> {
        val clean = word.lowercase().replace(Regex("[‘’`]"), "'")
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < clean.length) {
            if (clean[i] == '\'') { i++; continue }
            val v = vowels.firstOrNull { clean.startsWith(it, i) }
            if (v != null) { tokens.add(Token("vowel", v)); i += v.length; continue }
            val c = consonants.firstOrNull { clean.startsWith(it, i) }
            if (c != null) { tokens.add(Token("consonant", c)); i += c.length; continue }
            i++
        }
        return tokens
    }

    private fun romanWordToIndic(word: String, script: String): String {
        val cMap = if (script == "bn") bnConsonants else hiConsonants
        val vMap = if (script == "bn") bnVowels else hiVowels
        val virama = if (script == "bn") BN_VIRAMA else HI_VIRAMA
        val tokens = tokenize(word)
        val out = StringBuilder()
        for (idx in tokens.indices) {
            val token = tokens[idx]
            if (token.kind == "vowel") {
                val prev = tokens.getOrNull(idx - 1)
                val pair = vMap[token.value] ?: listOf(token.value, token.value)
                out.append(if (prev?.kind == "consonant") pair[1] else pair[0])
                continue
            }
            out.append(cMap[token.value] ?: token.value)
            val next = tokens.getOrNull(idx + 1)
            if (next?.kind == "consonant" && joinedConsonants.contains(token.value + next.value)) {
                out.append(virama)
            }
        }
        return out.toString()
    }

    private fun romanWordToUrdu(word: String): String {
        val tokens = tokenize(word)
        val out = StringBuilder()
        for (idx in tokens.indices) {
            val token = tokens[idx]
            if (token.kind == "consonant") {
                out.append(urConsonants[token.value] ?: token.value)
            } else {
                val prevConsonant = tokens.getOrNull(idx - 1)?.kind == "consonant"
                out.append(
                    if (prevConsonant) urAttachedVowels[token.value] ?: ""
                    else urInitialVowels[token.value] ?: ""
                )
            }
        }
        return out.toString()
    }

    private fun normalizeRoman(value: String): String {
        var s = value
            .replace(Regex("[āĀ]"), "aa").replace(Regex("[īĪ]"), "ii").replace(Regex("[ūŪ]"), "uu")
            .replace(Regex("[ḥḤ]"), "h").replace(Regex("[ṣṢ]"), "s").replace(Regex("[ḍḌ]"), "d")
            .replace(Regex("[ṭṬ]"), "t").replace(Regex("[ẓẒ]"), "z").replace(Regex("[ʿʾ]"), "'")
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), "")
        return s
    }

    private val romanPart = Regex("([A-Za-z'‘’`]+)")

    /**
     * Convert a Latin transliteration into the requested script.
     * Non-Roman parts (digits, punctuation) pass through untouched.
     */
    fun localize(value: String, script: String): String {
        val normalized = normalizeRoman(value)
        if (script == "en") return normalized
        if (script != "hi" && script != "bn" && script != "ur") return normalized
        val out = StringBuilder()
        var last = 0
        for (m in romanPart.findAll(normalized)) {
            out.append(normalized.substring(last, m.range.first))
            val part = m.value
            out.append(
                if (part.any { it.isLetter() }) {
                    if (script == "ur") romanWordToUrdu(part) else romanWordToIndic(part, script)
                } else part
            )
            last = m.range.last + 1
        }
        out.append(normalized.substring(last))
        return out.toString()
    }

    /** Default pronunciation script for an app language. */
    fun defaultScriptFor(lang: String): String = when (lang) {
        "hi" -> "hi"
        "bn" -> "bn"
        "ur" -> "ur"
        else -> "en"
    }
}
