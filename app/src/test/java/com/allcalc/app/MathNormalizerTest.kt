package com.allcalc.app

import com.allcalc.app.domain.scanner.MathNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathNormalizerTest {

    private val normalizer = MathNormalizer()

    @Test
    fun `converts word math to symbols`() {
        assertEquals("2 × 5", normalizer.prepare("two times five"))
        assertEquals("3 + 4", normalizer.prepare("3 plus 4"))
        assertEquals("10 - 2", normalizer.prepare("ten minus two"))
        assertEquals("8 ÷ 2", normalizer.prepare("8 divided by 2"))
    }

    @Test
    fun `handles percentage phrasing`() {
        val out = normalizer.prepare("25 percent of 800")
        assertTrue(out.contains("×"))
        assertFalse(out.contains("of"))
        assertTrue(out.contains("25"))
        assertTrue(out.contains("800"))
    }

    @Test
    fun `handles square roots in words`() {
        val out = normalizer.prepare("square root of 144")
        assertTrue(out.startsWith("√"))
        assertFalse(out.contains("of"))
    }

    @Test
    fun `fixes OCR letter-digit confusion`() {
        assertEquals("2×5", normalizer.prepare("2x5"))
        assertEquals("402", normalizer.prepare("40 2").replace(" ", ""))
        assertEquals("10", normalizer.prepare("1O").replace(" ", ""))
        assertEquals("805", normalizer.prepare("8O5").replace(" ", ""))
    }

    @Test
    fun `handles superscript exponents`() {
        assertEquals("x^2", normalizer.prepare("x²"))
        assertEquals("x^3", normalizer.prepare("x³"))
        assertEquals("10^-3", normalizer.prepare("10⁻³"))
    }

    @Test
    fun `removes thousands separators`() {
        assertEquals("1000000", normalizer.prepare("1,000,000"))
        assertEquals("1000.5", normalizer.prepare("1,000.5"))
    }

    @Test
    fun `strips question phrasing and trailing punctuation`() {
        assertEquals("sin(30)", normalizer.prepare("What is the value of sin(30)?"))
        assertEquals("2+2", normalizer.prepare("2+2."))
    }
}