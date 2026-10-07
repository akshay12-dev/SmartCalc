package com.allcalc.app.domain.specialized

import java.math.BigInteger
import kotlin.math.*

object StatisticsCalculators {

    // Parses comma, space, or newline separated numbers
    fun parseData(input: String): List<Double> {
        return input.split(",", " ", "\n", "\t", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toDoubleOrNull() }
    }

    // 1. Comprehensive Descriptive Statistics & Mean/Median/Mode/Range
    fun calculateDescriptiveStats(values: List<Double>): CalculationResult {
        if (values.isEmpty()) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Please enter at least one number")
        }

        val n = values.size
        val sorted = values.sorted()
        val sum = values.sum()
        val mean = sum / n

        // Median
        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }

        // Mode
        val frequencyMap = mutableMapOf<Double, Int>()
        for (v in values) {
            frequencyMap[v] = (frequencyMap[v] ?: 0) + 1
        }
        val maxFreq = frequencyMap.values.maxOrNull() ?: 1
        val modes = frequencyMap.filter { it.value == maxFreq && maxFreq > 1 }.keys.sorted()
        val modeStr = if (modes.isEmpty() || modes.size == values.distinct().size) {
            "No unique mode"
        } else {
            modes.joinToString(", ") { MathCalculators.formatNumber(it) }
        }

        val min = sorted.first()
        val max = sorted.last()
        val range = max - min

        // Variance & Standard Deviation
        var sumSquaredDiff = 0.0
        for (v in values) {
            val diff = v - mean
            sumSquaredDiff += diff * diff
        }

        val popVariance = sumSquaredDiff / n
        val popStdDev = sqrt(popVariance)

        val sampleVariance = if (n > 1) sumSquaredDiff / (n - 1) else 0.0
        val sampleStdDev = if (n > 1) sqrt(sampleVariance) else 0.0

        // Quartiles & IQR
        val q1 = percentile(sorted, 25.0)
        val q3 = percentile(sorted, 75.0)
        val iqr = q3 - q1

        val steps = listOf(
            "Count (n) = $n",
            "Sum (Σx) = ${MathCalculators.formatNumber(sum)}",
            "Mean (x̄) = Σx / n = ${MathCalculators.formatNumber(sum)} / $n = ${MathCalculators.formatNumber(mean)}",
            "Sum of squared differences: Σ(x - x̄)² = ${MathCalculators.formatNumber(sumSquaredDiff)}",
            "Sample Std Dev (s) = √(Σ(x - x̄)² / (n - 1)) = ${MathCalculators.formatNumber(sampleStdDev)}",
            "Population Std Dev (σ) = √(Σ(x - x̄)² / n) = ${MathCalculators.formatNumber(popStdDev)}"
        )

        val details = listOf(
            "Count (n)" to "$n",
            "Sum (Σx)" to MathCalculators.formatNumber(sum),
            "Mean (Average)" to MathCalculators.formatNumber(mean),
            "Median" to MathCalculators.formatNumber(median),
            "Mode" to modeStr,
            "Range" to MathCalculators.formatNumber(range),
            "Minimum" to MathCalculators.formatNumber(min),
            "Maximum" to MathCalculators.formatNumber(max),
            "Sample Std Dev (s)" to MathCalculators.formatNumber(sampleStdDev),
            "Sample Variance (s²)" to MathCalculators.formatNumber(sampleVariance),
            "Population Std Dev (σ)" to MathCalculators.formatNumber(popStdDev),
            "Population Variance (σ²)" to MathCalculators.formatNumber(popVariance),
            "Q1 (25th Percentile)" to MathCalculators.formatNumber(q1),
            "Q3 (75th Percentile)" to MathCalculators.formatNumber(q3),
            "IQR (Interquartile Range)" to MathCalculators.formatNumber(iqr)
        )

        return CalculationResult(
            primaryResult = "Mean: ${MathCalculators.formatNumber(mean)}, s: ${MathCalculators.formatNumber(sampleStdDev)}",
            details = details,
            steps = steps
        )
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val index = (p / 100.0) * (sorted.size - 1)
        val lower = floor(index).toInt()
        val upper = ceil(index).toInt()
        val weight = index - lower
        return sorted[lower] * (1.0 - weight) + sorted[upper] * weight
    }

    // 2. Permutation and Combination Calculator
    fun calculatePermutationCombination(n: Long, r: Long): CalculationResult {
        if (n < 0 || r < 0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "n and r must be non-negative")
        }
        if (r > n) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "r cannot be greater than n")
        }

        val nFact = factorial(n)
        val rFact = factorial(r)
        val nrFact = factorial(n - r)

        val nPr = nFact / nrFact
        val nCr = nPr / rFact

        // With repetition
        val nPrRep = BigInteger.valueOf(n).pow(r.toInt())
        val nCrRep = factorial(n + r - 1) / (rFact * factorial(n - 1))

        val steps = listOf(
            "Permutation without repetition: nPr = n! / (n - r)! = $n! / ($n - $r)! = $nPr",
            "Combination without repetition: nCr = n! / [r! · (n - r)!] = $n! / [$r! · ($n - $r)!] = $nCr",
            "Permutation with repetition: n^r = $n^$r = $nPrRep",
            "Combination with repetition: (n + r - 1)! / [r! · (n - 1)!] = $nCrRep"
        )

        return CalculationResult(
            primaryResult = "nPr = $nPr | nCr = $nCr",
            details = listOf(
                "Permutations nPr (Order matters)" to nPr.toString(),
                "Combinations nCr (Order doesn't matter)" to nCr.toString(),
                "Permutations with Repetition (n^r)" to nPrRep.toString(),
                "Combinations with Repetition" to nCrRep.toString()
            ),
            steps = steps
        )
    }

    private fun factorial(num: Long): BigInteger {
        var res = BigInteger.ONE
        for (i in 2..num) {
            res = res.multiply(BigInteger.valueOf(i))
        }
        return res
    }

    // 3. Probability Calculator
    fun calculateProbability(favorable: Double, total: Double): CalculationResult {
        if (total <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Total outcomes must be greater than zero")
        }
        if (favorable < 0.0 || favorable > total) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Favorable outcomes must be between 0 and total")
        }

        val prob = favorable / total
        val probPct = prob * 100.0
        val comp = 1.0 - prob
        val compPct = comp * 100.0

        val oddsFor = "${MathCalculators.formatNumber(favorable)} : ${MathCalculators.formatNumber(total - favorable)}"
        val oddsAgainst = "${MathCalculators.formatNumber(total - favorable)} : ${MathCalculators.formatNumber(favorable)}"

        val steps = listOf(
            "Formula: P(A) = Favorable Outcomes / Total Outcomes",
            "P(A) = $favorable / $total = ${MathCalculators.formatNumber(prob)} ($probPct%)",
            "Complement: P(A') = 1 - P(A) = 1 - ${MathCalculators.formatNumber(prob)} = ${MathCalculators.formatNumber(comp)} ($compPct%)",
            "Odds For: Favorable : Unfavorable = $oddsFor"
        )

        return CalculationResult(
            primaryResult = "${MathCalculators.formatNumber(probPct)}%",
            details = listOf(
                "Probability P(A)" to MathCalculators.formatNumber(prob),
                "Percentage" to "${MathCalculators.formatNumber(probPct)}%",
                "Complement P(not A)" to "${MathCalculators.formatNumber(compPct)}%",
                "Odds in Favor" to oddsFor,
                "Odds Against" to oddsAgainst
            ),
            steps = steps
        )
    }

    // 4. Z-Score Calculator: Z = (X - μ) / σ
    fun calculateZScore(x: Double, mean: Double, stdDev: Double): CalculationResult {
        if (stdDev <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Standard deviation (σ) must be greater than zero")
        }

        val z = (x - mean) / stdDev
        // Approximation of standard normal CDF (percentile)
        val p = standardNormalCdf(z) * 100.0

        val steps = listOf(
            "Formula: Z = (X - μ) / σ",
            "Z = ($x - $mean) / $stdDev = ${x - mean} / $stdDev = ${MathCalculators.formatNumber(z)}",
            "Percentile P(Z < ${MathCalculators.formatNumber(z)}) ≈ ${MathCalculators.formatNumber(p)}%"
        )

        return CalculationResult(
            primaryResult = "Z = ${MathCalculators.formatNumber(z)}",
            details = listOf(
                "Z-Score" to MathCalculators.formatNumber(z),
                "Percentile (Left Tail)" to "${MathCalculators.formatNumber(p)}%",
                "Right Tail P(Z > z)" to "${MathCalculators.formatNumber(100.0 - p)}%"
            ),
            steps = steps
        )
    }

    private fun standardNormalCdf(z: Double): Double {
        // Abramowitz and Stegun approximation
        val t = 1.0 / (1.0 + 0.2316419 * abs(z))
        val d = 0.3989422804014337 * exp(-z * z / 2.0)
        val prob = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        return if (z > 0) 1.0 - prob else prob
    }

    // 5. Confidence Interval Calculator: x̄ ± Z * (s / √n)
    fun calculateConfidenceInterval(mean: Double, stdDev: Double, n: Long, confidenceLevel: Double): CalculationResult {
        if (n <= 1L) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Sample size (n) must be greater than 1")
        }
        if (stdDev <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Standard deviation must be greater than zero")
        }

        // Critical value Z for standard levels
        val zCrit = when (confidenceLevel.toInt()) {
            90 -> 1.645
            95 -> 1.960
            99 -> 2.576
            else -> 1.960
        }

        val se = stdDev / sqrt(n.toDouble())
        val moe = zCrit * se
        val lower = mean - moe
        val upper = mean + moe

        val steps = listOf(
            "Confidence Level: $confidenceLevel% (Z* = $zCrit)",
            "Standard Error (SE) = s / √n = $stdDev / √$n = ${MathCalculators.formatNumber(se)}",
            "Margin of Error (MOE) = Z* · SE = $zCrit · ${MathCalculators.formatNumber(se)} = ${MathCalculators.formatNumber(moe)}",
            "Interval: x̄ ± MOE = [${MathCalculators.formatNumber(lower)}, ${MathCalculators.formatNumber(upper)}]"
        )

        return CalculationResult(
            primaryResult = "[${MathCalculators.formatNumber(lower)}, ${MathCalculators.formatNumber(upper)}]",
            details = listOf(
                "Confidence Interval" to "[${MathCalculators.formatNumber(lower)}, ${MathCalculators.formatNumber(upper)}]",
                "Margin of Error (±)" to MathCalculators.formatNumber(moe),
                "Standard Error" to MathCalculators.formatNumber(se),
                "Critical Value Z*" to "$zCrit"
            ),
            steps = steps
        )
    }

    // 6. Number Sequence Calculator (Arithmetic & Geometric)
    fun calculateSequence(type: String, a1: Double, diffOrRatio: Double, n: Long): CalculationResult {
        if (n <= 0L) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Term number (n) must be positive")
        }

        val steps = mutableListOf<String>()
        val details = mutableListOf<Pair<String, String>>()
        val nthTerm: Double
        val sumN: Double

        if (type.equals("Geometric", ignoreCase = true)) {
            val r = diffOrRatio
            nthTerm = a1 * r.pow((n - 1).toDouble())
            sumN = if (r == 1.0) {
                a1 * n
            } else {
                a1 * (1.0 - r.pow(n.toDouble())) / (1.0 - r)
            }
            steps.add("Geometric Progression: a₁ = $a1, common ratio r = $r")
            steps.add("n-th Term Formula: aₙ = a₁ · r^(n - 1) = $a1 · ($r)^($n - 1) = ${MathCalculators.formatNumber(nthTerm)}")
            steps.add("Sum of First n Terms: Sₙ = a₁ · (1 - r^n) / (1 - r) = ${MathCalculators.formatNumber(sumN)}")
        } else {
            // Arithmetic
            val d = diffOrRatio
            nthTerm = a1 + (n - 1) * d
            sumN = (n.toDouble() / 2.0) * (2 * a1 + (n - 1) * d)
            steps.add("Arithmetic Progression: a₁ = $a1, common difference d = $d")
            steps.add("n-th Term Formula: aₙ = a₁ + (n - 1)d = $a1 + ($n - 1)·$d = ${MathCalculators.formatNumber(nthTerm)}")
            steps.add("Sum of First n Terms: Sₙ = (n / 2) · (2a₁ + (n - 1)d) = ${MathCalculators.formatNumber(sumN)}")
        }

        details.add("Term $n (aₙ)" to MathCalculators.formatNumber(nthTerm))
        details.add("Sum of first $n terms (Sₙ)" to MathCalculators.formatNumber(sumN))

        return CalculationResult(
            primaryResult = "aₙ = ${MathCalculators.formatNumber(nthTerm)}",
            details = details,
            steps = steps
        )
    }

    // 7. Sample Size Calculator (Cochran's formula)
    fun calculateSampleSize(
        confidenceLevel: Double, // e.g. 95
        marginOfErrorPct: Double, // e.g. 5%
        population: Long? = null,
        pEstimate: Double = 0.5 // Default conservative 50%
    ): CalculationResult {
        if (marginOfErrorPct <= 0.0 || marginOfErrorPct >= 100.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Margin of error must be between 0% and 100%")
        }

        val zCrit = when (confidenceLevel.toInt()) {
            90 -> 1.645
            95 -> 1.960
            99 -> 2.576
            else -> 1.960
        }
        val e = marginOfErrorPct / 100.0
        val p = pEstimate.coerceIn(0.01, 0.99)

        // Cochran's formula: n0 = (Z^2 * p * (1-p)) / e^2
        val n0 = (zCrit * zCrit * p * (1.0 - p)) / (e * e)
        val finalN = if (population != null && population > 0L) {
            (n0 / (1.0 + (n0 - 1.0) / population)).toLong() + 1
        } else {
            ceil(n0).toLong()
        }

        val steps = listOf(
            "Cochran's Formula: n₀ = (Z² · p · (1 - p)) / e²",
            "Values: Z = $zCrit, p = $p, e = $e",
            "Unadjusted Sample Size n₀ = ${ceil(n0).toLong()}",
            if (population != null && population > 0L) {
                "Adjusted for Finite Population N = $population: n = n₀ / (1 + (n₀ - 1) / N) ≈ $finalN"
            } else {
                "Assuming infinite population"
            }
        )

        return CalculationResult(
            primaryResult = "Required Sample: $finalN",
            details = listOf(
                "Recommended Sample Size" to "$finalN",
                "Confidence Level" to "$confidenceLevel% (Z = $zCrit)",
                "Margin of Error" to "$marginOfErrorPct%",
                "Population" to (population?.toString() ?: "Infinite")
            ),
            steps = steps
        )
    }
}
