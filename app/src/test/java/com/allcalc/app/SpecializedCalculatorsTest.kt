package com.allcalc.app

import com.allcalc.app.domain.specialized.*
import org.junit.Assert.*
import org.junit.Test

class SpecializedCalculatorsTest {

    // --- MATH TESTS ---

    @Test
    fun testFractionAdditionAndSimplification() {
        val res = MathCalculators.calculateFraction(1, 2, 1, 4, "+")
        assertTrue(res.isSuccess)
        assertEquals("3/4", res.primaryResult)

        val resMultiply = MathCalculators.calculateFraction(2, 3, 3, 4, "*")
        assertTrue(resMultiply.isSuccess)
        assertEquals("1/2", resMultiply.primaryResult)

        val resDivZero = MathCalculators.calculateFraction(1, 2, 0, 1, "/")
        assertFalse(resDivZero.isSuccess)
    }

    @Test
    fun testPercentageCalculations() {
        // Mode 1: 15% of 200 = 30
        val res1 = MathCalculators.calculatePercentage(1, 15.0, 200.0)
        assertTrue(res1.isSuccess)
        assertEquals("30", res1.primaryResult)

        // Mode 2: 25 is what percent of 100? = 25%
        val res2 = MathCalculators.calculatePercentage(2, 25.0, 100.0)
        assertTrue(res2.isSuccess)
        assertEquals("25%", res2.primaryResult)

        // Mode 3: Change from 50 to 75 = 50% Increase
        val res3 = MathCalculators.calculatePercentage(3, 50.0, 75.0)
        assertTrue(res3.isSuccess)
        assertEquals("50% Increase", res3.primaryResult)
    }

    @Test
    fun testPercentError() {
        // Experimental 9.2, Theoretical 9.8 => |9.2 - 9.8| / 9.8 * 100% ≈ 6.122449%
        val res = MathCalculators.calculatePercentError(9.2, 9.8)
        assertTrue(res.isSuccess)
        assertTrue(res.primaryResult.startsWith("6.122"))
    }

    @Test
    fun testQuadraticFormula() {
        // x^2 - 5x + 6 = 0 => roots 3 and 2
        val res = MathCalculators.calculateQuadratic(1.0, -5.0, 6.0)
        assertTrue(res.isSuccess)
        assertTrue(res.primaryResult.contains("3") && res.primaryResult.contains("2"))

        // Repeated root: x^2 - 4x + 4 = 0 => x = 2
        val resDouble = MathCalculators.calculateQuadratic(1.0, -4.0, 4.0)
        assertTrue(resDouble.isSuccess)
        assertEquals("x = 2", resDouble.primaryResult)

        // Complex roots: x^2 + 1 = 0
        val resComplex = MathCalculators.calculateQuadratic(1.0, 0.0, 1.0)
        assertTrue(resComplex.isSuccess)
        assertTrue(resComplex.primaryResult.contains("i"))
    }

    @Test
    fun testLcmAndGcf() {
        val res = MathCalculators.calculateLcmGcf(listOf(24L, 36L, 60L))
        assertTrue(res.isSuccess)
        assertEquals("GCF = 12, LCM = 360", res.primaryResult)
    }

    @Test
    fun testFactors() {
        val res = MathCalculators.calculateFactors(28L)
        assertTrue(res.isSuccess)
        assertEquals("1, 2, 4, 7, 14, 28", res.primaryResult)
        assertTrue(res.details.any { it.first == "Prime?" && it.second.contains("Composite") })

        val primeRes = MathCalculators.calculateFactors(17L)
        assertTrue(primeRes.details.any { it.first == "Prime?" && it.second.contains("Prime") })
    }

    @Test
    fun testRatioSolver() {
        // 4 : 12 = 7 : D => D = 21
        val res = MathCalculators.calculateRatio(4.0, 12.0, 7.0, null)
        assertTrue(res.isSuccess)
        assertEquals("D = 21", res.primaryResult)
    }

    @Test
    fun testMatrix2x2() {
        // [ 1 2 ; 3 4 ] det = 1*4 - 2*3 = -2
        val res = MathCalculators.calculateMatrix2x2(1.0, 2.0, 3.0, 4.0)
        assertTrue(res.isSuccess)
        assertEquals("Det = -2", res.primaryResult)
    }

    @Test
    fun testBasesConversion() {
        val res = MathCalculators.calculateBases("255", 10)
        assertTrue(res.isSuccess)
        assertTrue(res.details.any { it.first.contains("Binary") && it.second == "11111111" })
        assertTrue(res.details.any { it.first.contains("Hexadecimal") && it.second == "FF" })
    }

    // --- STATISTICS TESTS ---

    @Test
    fun testDescriptiveStatistics() {
        val data = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        val res = StatisticsCalculators.calculateDescriptiveStats(data)
        assertTrue(res.isSuccess)
        // Mean = 40 / 8 = 5
        assertTrue(res.details.any { it.first.contains("Mean") && it.second == "5" })
        // Median = (4 + 5) / 2 = 4.5
        assertTrue(res.details.any { it.first.contains("Median") && it.second == "4.5" })
        // Mode = 4
        assertTrue(res.details.any { it.first.contains("Mode") && it.second == "4" })
        // Range = 9 - 2 = 7
        assertTrue(res.details.any { it.first.contains("Range") && it.second == "7" })
    }

    @Test
    fun testPermutationsAndCombinations() {
        // 5P3 = 60, 5C3 = 10
        val res = StatisticsCalculators.calculatePermutationCombination(5L, 3L)
        assertTrue(res.isSuccess)
        assertEquals("nPr = 60 | nCr = 10", res.primaryResult)
    }

    @Test
    fun testProbability() {
        val res = StatisticsCalculators.calculateProbability(1.0, 4.0)
        assertTrue(res.isSuccess)
        assertEquals("25%", res.primaryResult)
        assertTrue(res.details.any { it.first.contains("Complement") && it.second == "75%" })
    }

    @Test
    fun testZScore() {
        // X = 85, mean = 70, sd = 10 => Z = 1.5
        val res = StatisticsCalculators.calculateZScore(85.0, 70.0, 10.0)
        assertTrue(res.isSuccess)
        assertEquals("Z = 1.5", res.primaryResult)
    }

    @Test
    fun testSequences() {
        // Arithmetic: a1 = 3, d = 5, n = 4 => a4 = 3 + 3*5 = 18, sum = 4/2 * (6 + 15) = 42
        val res = StatisticsCalculators.calculateSequence("Arithmetic", 3.0, 5.0, 4L)
        assertTrue(res.isSuccess)
        assertEquals("aₙ = 18", res.primaryResult)
        assertTrue(res.details.any { it.first.contains("Sum") && it.second == "42" })
    }

    // --- GEOMETRY TESTS ---

    @Test
    fun testPythagoreanTheorem() {
        // a = 3, b = 4 => c = 5
        val res = GeometryCalculators.calculatePythagorean(3.0, 4.0, null)
        assertTrue(res.isSuccess)
        assertEquals("c = 5", res.primaryResult)

        // a = 3, c = 5 => b = 4
        val resLeg = GeometryCalculators.calculatePythagorean(3.0, null, 5.0)
        assertTrue(resLeg.isSuccess)
        assertEquals("b = 4", resLeg.primaryResult)
    }

    @Test
    fun testTriangleHeron() {
        // 3, 4, 5 triangle has area 6
        val res = GeometryCalculators.calculateTriangle(3.0, 4.0, 5.0)
        assertTrue(res.isSuccess)
        assertEquals("Area: 6", res.primaryResult)
    }

    @Test
    fun testCircleCalculator() {
        // radius = 10 => area = 100 * pi ≈ 314.159265
        val res = GeometryCalculators.calculateCircle("radius", 10.0)
        assertTrue(res.isSuccess)
        assertTrue(res.primaryResult.contains("314.159"))
    }

    @Test
    fun testVolumeSolid() {
        // Cube edge 3 => volume 27, surface area 54
        val res = GeometryCalculators.calculateSolid3D("cube", 3.0)
        assertTrue(res.isSuccess)
        assertTrue(res.primaryResult.contains("Volume: 27"))
        assertTrue(res.primaryResult.contains("Area: 54"))
    }

    @Test
    fun testSlopeAndMidpoint() {
        // (1, 2) to (5, 10) => m = (10 - 2) / (5 - 1) = 8 / 4 = 2
        val res = GeometryCalculators.calculateSlope(1.0, 2.0, 5.0, 10.0)
        assertTrue(res.isSuccess)
        assertEquals("Slope m = 2", res.primaryResult)
        assertTrue(res.details.any { it.first == "Midpoint" && it.second == "(3, 6)" })
    }

    @Test
    fun testDistance2DAnd3D() {
        // 3D (0, 0, 0) to (3, 4, 12) => sqrt(9 + 16 + 144) = sqrt(169) = 13
        val res = GeometryCalculators.calculateDistance(0.0, 0.0, 0.0, 3.0, 4.0, 12.0)
        assertTrue(res.isSuccess)
        assertEquals("Distance = 13", res.primaryResult)
    }

    // --- CATALOG & SEARCH TESTS ---

    @Test
    fun testSpecializedCatalog() {
        assertTrue(SpecializedCatalog.allCalculators.size >= 30)

        // Find by ID
        val frac = SpecializedCatalog.findById("fraction")
        assertNotNull(frac)
        assertEquals("Fraction Calculator", frac?.title)

        // Search by keyword
        val quad = SpecializedCatalog.search("parabola")
        assertTrue(quad.any { it.id == "quadratic" })

        val stats = SpecializedCatalog.search("", SpecializedCategory.STATISTICS)
        assertTrue(stats.isNotEmpty())
        assertTrue(stats.all { it.category == SpecializedCategory.STATISTICS })
    }
}
