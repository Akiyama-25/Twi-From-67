package Akari.Twitter.share.util

object DigitConverter {

    const val DEFAULT_S = "67"
    const val EASTER_EGG_VALUE = "67"
    const val EASTER_EGG_TOAST = "67676767"

    /**
     * Converts full-width digits (０-９, U+FF10..U+FF19) to half-width Arabic numerals (0-9).
     * Filters out any non-numeric characters.
     * Restricts output to at most 2 digits.
     */
    fun sanitizeToTwoDigits(input: CharSequence?): String {
        if (input.isNullOrEmpty()) return ""
        val sb = StringBuilder()
        for (i in 0 until input.length) {
            val ch = input[i]
            when (ch) {
                in '\uFF10'..'\uFF19' -> {
                    sb.append((ch.code - 0xFEE0).toChar())
                }
                in '0'..'9' -> {
                    sb.append(ch)
                }
            }
            if (sb.length == 2) break
        }
        return sb.toString()
    }

    /**
     * Validates whether input contains 1 or 2 Arabic digits.
     */
    fun isValidTwoDigits(input: String): Boolean {
        return input.isNotEmpty() && input.length <= 2 && input.all { it in '0'..'9' }
    }
}