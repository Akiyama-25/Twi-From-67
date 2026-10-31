package Akari.Twitter.share

import Akari.Twitter.share.util.DigitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitConverterTest {

    @Test
    fun testHalfWidthDigits() {
        assertEquals("67", DigitConverter.sanitizeToTwoDigits("67"))
        assertEquals("19", DigitConverter.sanitizeToTwoDigits("19"))
        assertEquals("1", DigitConverter.sanitizeToTwoDigits("1"))
        assertEquals("12", DigitConverter.sanitizeToTwoDigits("123"))
    }

    @Test
    fun testFullWidthDigits() {
        assertEquals("67", DigitConverter.sanitizeToTwoDigits("６７"))
        assertEquals("19", DigitConverter.sanitizeToTwoDigits("１９"))
        assertEquals("0", DigitConverter.sanitizeToTwoDigits("０"))
        assertEquals("46", DigitConverter.sanitizeToTwoDigits("４６"))
        assertEquals("12", DigitConverter.sanitizeToTwoDigits("１２３"))
    }

    @Test
    fun testMixedAndFilteredCharacters() {
        assertEquals("67", DigitConverter.sanitizeToTwoDigits("abc６7xyz"))
        assertEquals("19", DigitConverter.sanitizeToTwoDigits("?s=19"))
        assertEquals("", DigitConverter.sanitizeToTwoDigits("abc"))
    }

    @Test
    fun testValidation() {
        assertTrue(DigitConverter.isValidTwoDigits("67"))
        assertTrue(DigitConverter.isValidTwoDigits("19"))
        assertTrue(DigitConverter.isValidTwoDigits("0"))
        assertFalse(DigitConverter.isValidTwoDigits(""))
        assertFalse(DigitConverter.isValidTwoDigits("123"))
        assertFalse(DigitConverter.isValidTwoDigits("6a"))
    }
}