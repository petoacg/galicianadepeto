package gal.galiciana.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.text.Normalizer

val HighlightYellow = Color(0x66FFEB3B)
val HighlightInk = Color(0xFF1A1A1A)

private val QuoteMarks = charArrayOf('"', '“', '”', '«', '»', '„', '‟', '\'', '‘', '’')

fun stripSearchQuotes(raw: String): String =
    raw.trim().trim(*QuoteMarks).trim()

fun isQuotedQuery(raw: String): Boolean {
    val t = raw.trim()
    if (t.length < 2) return false
    val a = t.first()
    val b = t.last()
    val opens = setOf('"', '“', '«', '„', '‟', '\'', '‘')
    val closes = setOf('"', '”', '»', '\'', '’')
    return a in opens && b in closes
}

fun quotedPhrases(raw: String): List<String> {
    val out = mutableListOf<String>()
    Regex("""["“«]([^"”»]+)["”»]""").findAll(raw).forEach { m ->
        val p = m.groupValues[1].trim()
        if (p.contains(' ')) out += p
        else if (p.isNotBlank()) out += p
    }
    if (out.isEmpty() && isQuotedQuery(raw)) {
        val p = stripSearchQuotes(raw)
        if (p.isNotBlank()) out += p
    }
    return out
}

fun extraTokens(raw: String): List<String> {
    // A fully quoted expression can also use the supported single/low quote marks.
    // The standard expression below does not remove those fallback delimiters.
    if (isQuotedQuery(raw) && !Regex("""["“«]([^"”»]+)["”»]""").containsMatchIn(raw))
        return emptyList()
    val without = raw.replace(Regex("""["“«][^"”»]+["”»]"""), " ")
    return queryTokens(without)
}

fun queryTokens(query: String): List<String> =
    Regex("""[\p{L}\p{N}]+(?:[-‐‑–—­][\p{L}\p{N}]+)*""")
        .findAll(stripSearchQuotes(query)).map { it.value }.toList()

fun highlightTerms(query: String): String {
    val phrases = quotedPhrases(query)
    if (phrases.isNotEmpty()) {
        val extra = extraTokens(query)
        return (phrases.map { "\"$it\"" } + extra).joinToString(" ")
    }
    val kept = queryTokens(query)
    return kept.joinToString(" ").ifBlank { stripSearchQuotes(query) }
}

private val CombiningMarksPattern = Regex("\\p{M}+")
private val HyphenPattern = Regex("""[\-‐‑–—­]""")
private val HyphenLinePattern = Regex("""[\-‐‑–—­]\s*\n\s*""")
private val HyphenSpacePattern = Regex("""[\-‐‑–—­]\s+""")
private val NonSearchLetterPattern = Regex("[^a-z0-9ñ]")
private val UncertainOcrGlyphPattern = Regex("(?<=[a-zñ])[�□■#*?](?=[a-zñ])")
private val OcrWordPattern = Regex("[a-z0-9ñ?]+")

fun foldForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(CombiningMarksPattern, "")
        .lowercase()
        .replace(HyphenPattern, "")

/** Une «Mon-» + salto + «forte» y tira todos los guiones. */
fun dehyphen(s: String): String =
    s.replace(HyphenLinePattern, "")
        .replace(HyphenSpacePattern, "")
        .replace(HyphenPattern, "")

fun lettersOnly(s: String): String =
    foldForSearch(dehyphen(s)).replace(NonSearchLetterPattern, "")

/** Keep the shared OCR tokenization available to positioned ALTO searches. */
internal fun normalizedOcrWords(text: String): List<String> =
    foldForSearch(dehyphen(text))
        .replace(UncertainOcrGlyphPattern, "?")
        .let { OcrWordPattern.findAll(it).map { match -> match.value }.toList() }

/** A quoted single word must be a complete word with the same readable letters. */
fun containsStrictQuotedWord(text: String, word: String): Boolean {
    val target = lettersOnly(word)
    return target.isNotEmpty() && normalizedOcrWords(text).any { it == target }
}

private fun uncertainGlyphMatch(ocr: String, query: String): Boolean =
    query.length >= 4 && ocr.length == query.length && ocr.count { it == '?' } == 1 &&
        ocr.indices.all { ocr[it] == '?' || ocr[it] == query[it] }

private fun commonOcrConfusion(ocr: String, query: String): Boolean {
    if (query.length < 4) return false
    val pairs = listOf("0" to "o", "1" to "l", "1" to "i", "5" to "s", "rn" to "m", "vv" to "w")
    return pairs.any { (a, b) ->
        ocr.replace(a, b) == query || ocr.replace(b, a) == query
    }
}

fun editDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (kotlin.math.abs(a.length - b.length) > 2) return 9
    val m = a.length
    val n = b.length
    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..n) {
            val cur = dp[j]
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
            prev = cur
        }
    }
    return dp[n]
}

/**
 * slack: 0 = solo guiones, manchas no-letra y la palabra exacta.
 * 1 = una letra de más/menos o una sustitución en términos largos.
 */
fun tokenMatches(ocr: String, query: String, slack: Int): Boolean {
    val q = lettersOnly(query)
    val o = foldForSearch(dehyphen(ocr))
    if (q.length < 2) return false
    if (o == q) return true
    if (uncertainGlyphMatch(o, q) || commonOcrConfusion(o, q)) return true
    val masked = foldForSearch(dehyphen(ocr)).map { ch ->
        if (ch in 'a'..'z' || ch == 'ñ' || ch.isDigit()) ch else '.'
    }.joinToString("").replace(Regex("\\.+"), ".")
    if (masked.contains('.') && q.length >= 4) {
        val pat = masked.split('.').filter { it.isNotEmpty() }.joinToString("[a-zñ]?") { Regex.escape(it) }
        if (pat.length >= 3 && Regex("^(?:$pat)$").matches(q)) return true
    }
    if (slack <= 0) return false
    // A one-letter error in a long term is useful recall; short, different words
    // must not become a match just because another query term also appears.
    if (q.length >= 5 && o.length >= 4 && editDistance(o, q) <= slack) return true
    if (slack >= 1 && (o == q + "s" || q == o + "s" || o == q + "es" || q == o + "es")) return true
    return false
}

fun slackForQuery(tokens: List<String>, token: String): Int {
    val length = lettersOnly(token).length
    return when {
        tokens.size >= 2 && length >= 5 -> 1
        tokens.size == 1 && length >= 10 -> 1
        else -> 0
    }
}

private fun tokenToPattern(token: String): String {
    val core = buildString {
        for (ch in token) {
            append(
                when (foldForSearch(ch.toString()).firstOrNull()) {
                    'a' -> "[aáàäâ]"
                    'e' -> "[eéèëê]"
                    'i' -> "[iíìïî]"
                    'o' -> "[oóòöô]"
                    'u' -> "[uúùüû]"
                    'n' -> "[nñ]"
                    else -> Regex.escape(ch.toString())
                }
            )
            append("""[-‐‑–—­]?\s*""")
        }
    }
    return """(?<![\p{L}\p{N}])$core(?![\p{L}\p{N}])"""
}

fun highlightRegex(query: String): Regex? {
    val phrases = quotedPhrases(query)
    val phraseGap = """[\s.,;:·\-–—'’"“”\n]+"""
    if (phrases.isNotEmpty()) {
        val parts = phrases.map { ph ->
            ph.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(phraseGap) { tokenToPattern(it) }
        } + extraTokens(query).map { tokenToPattern(it) }
        return Regex(parts.joinToString("|"), setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    }
    val tokens = queryTokens(query)
    if (tokens.isEmpty()) return null
    if (isQuotedQuery(query) && tokens.size > 1) {
        return Regex(
            tokens.joinToString(phraseGap) { tokenToPattern(it) },
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )
    }
    val parts = buildList {
        if (tokens.size > 1) add(tokens.joinToString(phraseGap) { tokenToPattern(it) })
        tokens.forEach { add(tokenToPattern(it)) }
    }
    return Regex(parts.joinToString("|"), setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
}

fun highlightQuery(text: String, query: String): AnnotatedString {
    val regex = highlightRegex(query)
    if (regex == null || text.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var last = 0
        regex.findAll(text).forEach { m ->
            if (m.range.first < last) return@forEach
            append(text.substring(last, m.range.first))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    background = Color(0xFFFFF59D),
                    color = HighlightInk,
                )
            ) { append(m.value) }
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}

/** Mark the OCR words that justify a broadened search, including likely scan errors. */
fun highlightPossibleTerms(text: String, query: String): AnnotatedString {
    val tokens = queryTokens(query).map(::lettersOnly).filter { it.length >= 2 }.distinct()
    if (tokens.isEmpty()) return AnnotatedString(text)
    val wordPattern = Regex("""[\p{L}\p{N}�□■#*?]+(?:[-‐‑–—­]\s*[\p{L}\p{N}]+)*""")
    return buildAnnotatedString {
        var last = 0
        wordPattern.findAll(text).forEach { match ->
            if (match.range.first < last) return@forEach
            append(text.substring(last, match.range.first))
            val likely = tokens.any { token ->
                tokenMatches(match.value, token, slackForQuery(tokens, token))
            }
            if (likely) withStyle(SpanStyle(
                fontWeight = FontWeight.Bold,
                background = Color(0xFFFFE082),
                color = HighlightInk,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            )) { append(match.value) }
            else append(match.value)
            last = match.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}

fun foldedBlob(text: String): String =
    foldForSearch(dehyphen(text)).replace(Regex("[^a-z0-9ñ\\s]+"), " ").replace(Regex("\\s+"), " ").trim()

private fun matchAt(words: List<String>, start: Int, token: String, slack: Int): Int? {
    if (start >= words.size) return null
    if (lettersOnly(token).length == 1) return if (words[start] == lettersOnly(token)) start + 1 else null
    if (tokenMatches(words[start], token, slack)) return start + 1
    // OCR can split a word in two even after a hyphen has disappeared.
    if (start + 1 < words.size && words[start].length >= 2 && words[start + 1].length >= 2 &&
        words[start].length <= 5 &&
        words[start].length + words[start + 1].length <= lettersOnly(token).length + 1 &&
        tokenMatches(words[start] + words[start + 1], token, 0)) return start + 2
    return null
}

/** Ranges use pre-normalized OCR tokens and query terms; no matching rule is relaxed. */
internal fun phraseMatchRanges(words: List<String>, expected: List<String>): List<IntRange> {
    if (expected.isEmpty()) return emptyList()
    if (expected.size == 1) return words.indices.filter { words[it] == expected.single() }.map { it..it }
    val costs = HashMap<Pair<String, String>, Int>()
    fun matchCost(ocr: String, target: String): Int = costs.getOrPut(ocr to target) {
        when {
            ocr == target -> 0
            target.length >= 3 && ocr.count { it == '?' } == 1 -> {
                val gap = ocr.indexOf('?')
                if ((ocr.length == target.length + 1 && ocr.removeRange(gap, gap + 1) == target) ||
                    (ocr.length == target.length && ocr.indices.all { it == gap || ocr[it] == target[it] })) 0
                else if (target.length >= 5 && ocr.length >= 4 && editDistance(ocr, target) == 1) 1 else 2
            }
            target.length >= 5 && ocr.length >= 4 && editDistance(ocr, target) == 1 -> 1
            else -> 2
        }
    }
    val ranges = mutableListOf<IntRange>()
    for (start in words.indices) {
        var cursor = start
        var errors = 0
        val matched = expected.all { token ->
            if (cursor >= words.size) false
            else {
                val direct = matchCost(words[cursor], token)
                val joined = if (cursor + 1 < words.size && words[cursor].length in 2..5)
                    matchCost(words[cursor] + words[cursor + 1], token) else 2
                when {
                    direct <= 1 && errors + direct <= 1 -> { errors += direct; cursor++; true }
                    joined <= 1 && errors + joined <= 1 -> { errors += joined; cursor += 2; true }
                    else -> false
                }
            }
        }
        if (matched) ranges += start until cursor
    }
    return ranges
}

private fun phraseCount(text: String, phrase: String): Int = phraseMatchRanges(
    normalizedOcrWords(text), queryTokens(phrase).map(::lettersOnly).filter { it.isNotEmpty() },
).size

fun containsPhrase(text: String, query: String): Boolean {
    return phraseCount(text, query) > 0
}

/** Las frases admiten un corte OCR y, si tienen varias palabras, un único error
 * de letra legible en toda la frase. Una palabra sola entre comillas es literal.
 */
fun containsOcrLiteralPhrase(text: String, phrase: String): Boolean {
    return phraseCount(text, phrase) > 0
}

fun countMatches(text: String, query: String): Int {
    if (text.isEmpty()) return 0
    val phrases = quotedPhrases(query)
    val extras = extraTokens(query)
    val words = normalizedOcrWords(text)
    return phrases.sumOf { phraseMatchRanges(words, queryTokens(it).map(::lettersOnly).filter(String::isNotEmpty)).size } +
        extras.sumOf { tok -> words.indices.count { matchAt(words, it, tok, slackForQuery(extras, tok)) != null } }
}

fun textContainsQuery(text: String, query: String): Boolean {
    val phrases = quotedPhrases(query)
    val extras = extraTokens(query)
    if (phrases.isEmpty() && extras.isEmpty()) return false
    val words = normalizedOcrWords(text)
    if (phrases.any { phraseMatchRanges(words, queryTokens(it).map(::lettersOnly).filter(String::isNotEmpty)).isEmpty() }) return false
    return extras.all { token ->
        words.indices.any { matchAt(words, it, token, slackForQuery(extras, token)) != null }
    }
}
