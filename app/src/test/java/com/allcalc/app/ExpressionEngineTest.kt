package com.allcalc.app

import com.allcalc.app.domain.calculator.EvalResult
import com.allcalc.app.domain.calculator.EvaluationException
import com.allcalc.app.domain.calculator.ExpressionEngine
import com.allcalc.app.domain.calculator.formatSmart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpressionEngineTest {

    private val engine = ExpressionEngine()

    private fun evaluate(expr: String): Double {
        val result = try {
            engine.evaluate(expr)
        } catch (e: com.allcalc.app.domain.calculator.EvaluationException) {
            throw AssertionError("evaluate failed for: \"$expr\" -> ${e.message}", e)
        }
        return when (result) {
            is EvalResult.Value -> result.value.toDouble()
            is EvalResult.Solved -> result.value.toDouble()
            is EvalResult.Equality -> throw AssertionError("Expected a value result for: $expr")
        }
    }

    private fun assertValue(expr: String, expected: Double, delta: Double = 1e-6) {
        assertEquals(expr, expected, evaluate(expr), delta)
    }

    @Test
    fun `evaluates basic arithmetic`() {
        assertValue("25×12+50÷2", 325.0)
        assertValue("2+3*4", 14.0)
        assertValue("10÷2", 5.0)
        assertValue("25×12+50÷2", 325.0)
    }

    @Test
    fun `evaluates powers and roots`() {
        assertValue("2^10", 1024.0)
        assertValue("√144", 12.0)
        assertValue("sqrt(16)", 4.0)
        assertValue("√(25)", 5.0)
    }

    @Test
    fun `evaluates trig with degrees by default`() {
        assertValue("sin(30)", 0.5)
        assertValue("sin(90)", 1.0)
        assertValue("cos(0)", 1.0)
        assertValue("tan(45)", 1.0, 1e-6)
    }

    @Test
    fun `evaluates trig with radians`() {
        val res = engine.evaluate("sin(π÷2)", angleModeDeg = false) as EvalResult.Value
        assertEquals(1.0, res.value.toDouble(), 1e-6)
        assertValue("cos(0)", 1.0)
    }

    @Test
    fun `evaluates percent both as postfix and binary`() {
        assertValue("10%", 0.1)
        assertValue("25% of 800", 200.0)
        assertValue("25 % × 800", 200.0)
    }

    @Test
    fun `evaluates implicit multiplication and factorial`() {
        assertValue("2(3+4)", 14.0)
        assertValue("2π", 6.283185307179586, 1e-6)
        assertValue("5!", 120.0)
        assertValue("0.1+0.2", 0.3, 1e-9)
    }

    @Test
    fun `evaluates constants`() {
        assertValue("π", 3.141592653589793, 1e-6)
        assertValue("e", 2.718281828459045, 1e-6)
    }

    @Test
    fun `evaluates logarithmic and absolute functions`() {
        assertValue("ln(e)", 1.0, 1e-6)
        assertValue("log(100)", 2.0, 1e-6)
        assertValue("|−5|", 5.0)
        assertValue("abs(−3)", 3.0)
    }

    @Test
    fun `autocomplete closes brackets`() {
        assertEquals("sin(30)", engine.autocomplete("sin(30"))
        assertEquals("(2+3)", engine.autocomplete("(2+3"))
    }

    @Test
    fun `verifies equalities`() {
        val result = engine.evaluate("3+4=7")
        assertTrue(result is EvalResult.Equality)
        assertTrue((result as EvalResult.Equality).holds)

        val falseResult = engine.evaluate("2+2=5")
        assertTrue(falseResult is EvalResult.Equality)
        assertTrue(!(falseResult as EvalResult.Equality).holds)
    }

    @Test
    fun `solves single variable linear equations`() {
        val result = engine.evaluate("2x+5=15", allowVariables = true)
        assertTrue(result is EvalResult.Solved)
        val solved = result as EvalResult.Solved
        assertEquals('x', solved.variable)
        assertEquals(5.0, solved.value.toDouble(), 1e-6)
    }

    @Test
    fun `solves equations with a variable on the right`() {
        val result = engine.evaluate("7=x+2", allowVariables = true)
        assertTrue(result is EvalResult.Solved)
        assertEquals(5.0, (result as EvalResult.Solved).value.toDouble(), 1e-6)
    }

    @Test
    fun `rejects division by zero`() {
        try {
            engine.evaluate("2÷0")
            fail("Expected EvaluationException")
        } catch (e: EvaluationException) {
            // expected
        }
    }

    @Test
    fun `rejects unbalanced operators`() {
        try {
            engine.evaluate("2+")
            fail("Expected EvaluationException")
        } catch (e: EvaluationException) {
            // expected
        }
    }

    @Test
    fun `formatSmart collapses long decimals`() {
        assertEquals("0.3", formatSmart(java.math.BigDecimal("0.300000000000"), 12))
        assertEquals("325", formatSmart(java.math.BigDecimal("325.000000000000"), 12))
    }
}