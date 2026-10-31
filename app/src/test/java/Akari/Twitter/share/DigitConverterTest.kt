package Akari.Twitter.share

import Akari.Twitter.share.util.DigitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitConverterTest {

    @Test
    fun testHalfWidthDigitsWithoutLengthLimit() {
        assertEquals("67", DigitConverter.sanitizeDigits("67"))
        assertEquals("19", DigitConverter.sanitizeDigits("19"))
        assertEquals("1", DigitConverter.sanitizeDigits("1"))
        assertEquals("1234567890", DigitConverter.sanitizeDigits("1234567890"))
    }

    @Test
    fun testFullWidthDigits() {
        assertEquals("67", DigitConverter.sanitizeDigits("６７"))
        assertEquals("19", DigitConverter.sanitizeDigits("１９"))
        assertEquals("0", DigitConverter.sanitizeDigits("０"))
        assertEquals("46", DigitConverter.sanitizeDigits("４６"))
        assertEquals("123456", DigitConverter.sanitizeDigits("１２３４５６"))
    }

    @Test
    fun testMixedAndFilteredCharacters() {
        assertEquals("678", DigitConverter.sanitizeDigits("abc６78xyz"))
        assertEquals("19", DigitConverter.sanitizeDigits("?s=19"))
        assertEquals("", DigitConverter.sanitizeDigits("abc"))
    }

    @Test
    fun testValidation() {
        assertTrue(DigitConverter.isValidDigits("67"))
        assertTrue(DigitConverter.isValidDigits("19"))
        assertTrue(DigitConverter.isValidDigits("0"))
        assertTrue(DigitConverter.isValidDigits("12345678901234567890"))
        assertFalse(DigitConverter.isValidDigits(""))
        assertFalse(DigitConverter.isValidDigits("6a"))
    }
}