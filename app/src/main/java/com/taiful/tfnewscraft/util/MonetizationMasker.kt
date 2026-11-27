package com.taiful.tfnewscraft.util

/**
 * Facebook Monetization Safety Masking Engine — restored from the original app's v4/v5.1 rules.
 * Masks sensitive Bengali and English words (e.g., মাদক -> মা*দক, হত্যা -> হ*ত্যা) so news pages
 * stay compliant with Facebook monetization guidelines.
 */
object MonetizationMasker {

    val MASK_SYMBOLS: List<String> = listOf("*", "•", "-", ".", "'")

    val DEFAULT_SENSITIVE_WORDS: List<String> = listOf(
        // Bengali sensitive news terms
        "ধর্ষণ", "ধর্ষক", "ধর্ষিত", "গণধর্ষণ", "যৌন হয়রানি", "যৌন নির্যাতন", "যৌনতা",
        "শ্লীলতাহানি", "পর্নো", "হত্যা", "হত্যাকাণ্ড", "গণহত্যা", "খুন", "খুনি",
        "আত্মহত্যা", "আত্মঘাতী", "লাশ", "মরদেহ", "মৃতদেহ", "রক্তাক্ত", "জবাই",
        "গলাকাটা", "কুপিয়ে", "পিটিয়ে", "ছুরিকাঘাত", "নিহত", "নির্যাতন", "হামলা",
        "সংঘর্ষ", "সহিংসতা", "মামলা", "গ্রেপ্তার", "গ্রেফতার", "অপহরণ", "যুদ্ধ",
        "বোমা", "বিস্ফোরণ", "ক্ষেপণাস্ত্র", "মিসাইল", "গুলিবিদ্ধ", "গুলিতে",
        "গুলি চালিয়ে", "গুলি করে", "গোলাগুলি", "অস্ত্র", "সন্ত্রাস", "জঙ্গি",
        "হুতি", "হামাস", "হিজবুল্লাহ", "তালেবান", "আইএস", "আল-কায়েদা",
        "মাদক", "ইয়াবা", "হেরোইন",
        // English sensitive news terms
        "rape", "raped", "rapist", "sexual assault", "sexual abuse", "harassment",
        "murder", "murdered", "killed", "killing", "kill", "suicide", "dead body",
        "corpse", "bloody", "stabbed", "shooting", "shot dead", "gunfire", "bomb",
        "bombing", "blast", "explosion", "missile", "airstrike", "war", "genocide",
        "massacre", "terror", "terrorist", "terrorism", "militant", "houthi",
        "houthis", "hamas", "hezbollah", "taliban", "isis", "al-qaeda", "drugs",
        "abduction", "kidnap", "kidnapped", "torture", "arrested", "attack",
        "attacked", "violence"
    )

    data class MaskResult(
        val maskedText: String,
        val matchedWords: List<String>
    )

    fun findSensitiveWords(
        text: String,
        words: List<String> = DEFAULT_SENSITIVE_WORDS
    ): List<String> {
        if (text.isBlank()) return emptyList()
        return words
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filter { w -> text.contains(w, ignoreCase = true) }
            .distinct()
            .toList()
    }

    fun maskText(
        text: String,
        maskSymbol: String = "*",
        words: List<String> = DEFAULT_SENSITIVE_WORDS
    ): MaskResult {
        if (text.isBlank()) return MaskResult(text, emptyList())
        val sym = maskSymbol.ifBlank { "*" }
        val sortedWords = words
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
            .toList()

        var output = text
        val matched = mutableListOf<String>()

        for (word in sortedWords) {
            val regex = Regex(Regex.escape(word), RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(output)) {
                matched.add(word)
                output = regex.replace(output) { m ->
                    insertMaskSymbol(m.value, sym)
                }
            }
        }
        return MaskResult(output, matched.distinct())
    }

    private fun insertMaskSymbol(word: String, symbol: String): String {
        if (word.length <= 2) return "${word.first()}$symbol${word.drop(1)}"
        val splitIndex = (word.length / 2).coerceAtLeast(1)
        return word.substring(0, splitIndex) + symbol + word.substring(splitIndex)
    }
}
