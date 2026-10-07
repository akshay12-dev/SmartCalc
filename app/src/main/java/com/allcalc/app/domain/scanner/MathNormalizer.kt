package com.allcalc.app.domain.scanner

class MathNormalizer {

    private val wordOps = listOf(
        "what is the value of" to "",
        "what is the result of" to "",
        "compute the value of" to "",
        "calculate the value of" to "",
        "solve the equation" to "",
        "the square root of" to "√",
        "square root of" to "√",
        "what is" to "",
        "calculate" to "",
        "evaluate" to "",
        "simplify" to "",
        "find" to "",
        "multiplied by" to "×",
        "divide by" to "÷",
        "divided by" to "÷",
        "multiply by" to "×",
        "multiply" to "×",
        "percent of" to "% of",
        "percent" to "%",
        "is equal to" to "=",
        "equal to" to "=",
        "equals" to "=",
        "times" to "×",
        "plus" to "+",
        "minus" to "-",
        "half of" to "(1/2)×",
        "half" to "(1/2)",
        "quarter of" to "(1/4)×",
        "quarter" to "(1/4)"
    )

    private val numberWords = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
        "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
        "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
        "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
        "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000",
        "million" to "1000000"
    )

    private val superscripts = mapOf(
        '⁰' to "0", '¹' to "1", '²' to "2", '³' to "3", '⁴' to "4", '⁵' to "5",
        '⁶' to "6", '⁷' to "7", '⁸' to "8", '⁹' to "9", '⁻' to "-"
    )

    /**
     * Turns raw OCR output or hand-typed math text into a string the expression engine
     * accepts. It fixes common OCR confusions and simple word-based math, but never
     * silently reinterprets the core expression - the user always sees and edits it.
     */
    fun prepare(text: String): String {
        var s = text.trim()
        s = replaceWordOps(s)
        s = translateWordNumbers(s)
        s = handleOfConcept(s)
        s = unicodeSuperscripts(s)
        s = normalizeSymbols(s)
        s = fixDigitNeighbourLetters(s)
        s = fixDigitTimes(s)
        s = removeThousandsSeparators(s)
        s = stripPunctuation(s)
        return s.trim()
    }

    private fun replaceWordOps(s: String): String {
        var r = s
        for ((word, replacement) in wordOps) {
            r = r.replace(Regex(Regex.escape(word), RegexOption.IGNORE_CASE), replacement)
        }
        return r
    }

    private fun handleOfConcept(s: String): String =
        s.replace(Regex("\\bof\\b", RegexOption.IGNORE_CASE), "×")

    private fun translateWordNumbers(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isLetter()) {
                val start = i
                while (i < s.length && s[i].isLetter()) i++
                val word = s.substring(start, i).lowercase()
                out.append(numberWords[word] ?: word)
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun unicodeSuperscripts(s: String): String {
        val out = StringBuilder()
        var inExponent = false
        for (ch in s) {
            val rep = superscripts[ch]
            if (rep != null) {
                if (!inExponent) {
                    out.append('^')
                    inExponent = true
                }
                out.append(rep)
            } else {
                out.append(ch)
                inExponent = false
            }
        }
        return out.toString()
    }

    private fun normalizeSymbols(s: String): String {
        var r = s
        r = r.replace('−', '-').replace('–', '-').replace('—', '-')
        r = r.replace('∕', '/').replace('⁄', '/')
        r = r.replace('（', '(').replace('）', ')')
        r = r.replace('·', '×')
        r = r.replace('⋆', '×')
        return r
    }

    private fun fixDigitNeighbourLetters(s: String): String {
        fun map(m: MatchResult): String = when (m.value) {
            "O", "o" -> "0"
            "I", "l" -> "1"
            "B" -> "8"
            "S" -> "5"
            else -> m.value
        }
        var r = s.replace(Regex("(?<=\\d)[OoIlBS]")) { map(it) }
        r = r.replace(Regex("[OoIlBS](?=\\d)")) { map(it) }
        return r
    }

    private fun fixDigitTimes(s: String): String =
        s.replace(Regex("(?<=\\d)[xX](?=\\d)"), "×")

    private fun removeThousandsSeparators(s: String): String =
        s.replace(Regex("(?<=\\d),(?=\\d)"), "")

    private fun stripPunctuation(s: String): String =
        s.trim().trim('?', '!', '.', ':', ';', ',', ' ')
}