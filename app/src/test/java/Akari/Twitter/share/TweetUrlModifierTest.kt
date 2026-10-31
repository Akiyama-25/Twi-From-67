package Akari.Twitter.share

import Akari.Twitter.share.hook.TweetUrlModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TweetUrlModifierTest {

    @Test
    fun testModifySingleDigit() {
        val url = "https://x.com/MIT_CSAIL/status/1363172815315214336?s=19"
        val modified = TweetUrlModifier.modify(url, "8", false)
        assertTrue(modified.contains("s=8"))
    }

    @Test
    fun testModifyTwoDigits() {
        val url = "https://x.com/MIT_CSAIL/status/1363172815315214336?s=19"
        val modified = TweetUrlModifier.modify(url, "46", false)
        assertTrue(modified.contains("s=46"))
    }

    @Test
    fun testModifyThreeDigits() {
        val url = "https://x.com/MIT_CSAIL/status/1363172815315214336?s=19"
        val modified = TweetUrlModifier.modify(url, "114", false)
        assertTrue(modified.contains("s=114"))
    }

    @Test
    fun testModifyArbitraryLengthDigits() {
        val url = "https://x.com/MIT_CSAIL/status/1363172815315214336?a=abc&s=19"
        val modified = TweetUrlModifier.modify(url, "123456789", false)
        assertTrue(modified.contains("s=123456789"))
        assertTrue(modified.contains("a=abc"))
    }

    @Test
    fun testCleanTracking() {
        val url = "https://x.com/MIT_CSAIL/status/1363172815315214336?a=abc&t=xyz&s=19"
        val modified = TweetUrlModifier.modify(url, "114", true)
        assertEquals("https://x.com/MIT_CSAIL/status/1363172815315214336?s=114", modified)
    }
}