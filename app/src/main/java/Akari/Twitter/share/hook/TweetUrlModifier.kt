package Akari.Twitter.share.hook

import android.net.Uri
import java.util.regex.Pattern

object TweetUrlModifier {

    val isRewriting = ThreadLocal<Boolean>()

    // Matches tweet/status URLs from x.com or twitter.com
    private val TWEET_URL_REGEX = Pattern.compile(
        "https?://(?:www\\.)?(?:x|twitter)\\.com/[A-Za-z0-9_]+/status/[0-9]+(?:\\?[^\\s\"'<>]*)?",
        Pattern.CASE_INSENSITIVE
    )

    fun containsTweetUrl(text: String): Boolean {
        return TWEET_URL_REGEX.matcher(text).find()
    }

    /**
     * Rewrites all tweet status URLs found inside the given text.
     * Replaces or appends the '?s=' parameter with targetS.
     * Optionally strips other tracking parameters (like 'a=', 't=').
     */
    fun modify(originalText: String, targetS: String, cleanTracking: Boolean = false): String {
        val effectiveS = targetS.ifBlank { "67" }
        val matcher = TWEET_URL_REGEX.matcher(originalText)
        val sb = StringBuffer()

        isRewriting.set(true)
        try {
            while (matcher.find()) {
                val matchedUrl = matcher.group(0)
                val rewritten = rewriteSingleUrl(matchedUrl, effectiveS, cleanTracking)
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rewritten))
            }
            matcher.appendTail(sb)
            return sb.toString()
        } finally {
            isRewriting.set(false)
        }
    }

    private fun rewriteSingleUrl(rawUrl: String, targetS: String, cleanTracking: Boolean): String {
        return try {
            val uri = Uri.parse(rawUrl)
            val builder = uri.buildUpon().clearQuery()

            val queryParamNames = uri.queryParameterNames
            var hasS = false

            for (param in queryParamNames) {
                when {
                    param == "s" -> {
                        builder.appendQueryParameter("s", targetS)
                        hasS = true
                    }
                    cleanTracking && (param == "a" || param == "t" || param.startsWith("ref_")) -> {
                        // Skip tracking parameters when cleanTracking is enabled
                    }
                    else -> {
                        for (value in uri.getQueryParameters(param)) {
                            builder.appendQueryParameter(param, value)
                        }
                    }
                }
            }

            if (!hasS) {
                builder.appendQueryParameter("s", targetS)
            }

            builder.build().toString()
        } catch (_: Exception) {
            rawUrl
        }
    }
}