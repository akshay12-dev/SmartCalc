package com.allcalc.app.domain.specialized

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

// Result model for all specialized calculations
data class CalculationResult(
    val primaryResult: String,
    val details: List<Pair<String, String>> = emptyList(),
    val steps: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object MathCalculators {

    // 1. Fraction Calculator
    fun calculateFraction(
        n1: Long, d1: Long,
        n2: Long, d2: Long,
        operation: String // "+", "-", "*", "/"
    ): CalculationResult {
        if (d1 == 0L || d2 == 0L) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Denominator cannot be zero")
        }
        if (operation == "/" && n2 == 0L) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Cannot divide by zero fraction")
        }

        var resNum: Long
        var resDen: Long
        val steps = mutableListOf<String>()

        when (operation) {
            "+" -> {
                resNum = n1 * d2 + n2 * d1
                resDen = d1 * d2
                steps.add("Formula: (a/b) + (c/d) = (a·d + b·c) / (b·d)")
                steps.add("Cross multiply: ($n1 · $d2) + ($d1 · $n2) = ${n1 * d2} + ${d1 * n2} = $resNum")
                steps.add("Common denominator: $d1 · $d2 = $resDen")
            }
            "-" -> {
                resNum = n1 * d2 - n2 * d1
                resDen = d1 * d2
                steps.add("Formula: (a/b) - (c/d) = (a·d - b·c) / (b·d)")
                steps.add("Cross multiply: ($n1 · $d2) - ($d1 · $n2) = ${n1 * d2} - ${d1 * n2} = $resNum")
                steps.add("Common denominator: $d1 · $d2 = $resDen")
            }
            "*" -> {
                resNum = n1 * n2
                resDen = d1 * d2
                steps.add("Formula: (a/b) · (c/d) = (a·c) / (b·d)")
                steps.add("Numerator: $n1 · $n2 = $resNum")
                steps.add("Denominator: $d1 · $d2 = $resDen")
            }
            "/" -> {
                resNum = n1 * d2
                resDen = d1 * n2
                steps.add("Formula: (a/b) ÷ (c/d) = (a/b) · (d/c)")
                steps.add("Invert second fraction and multiply: ($n1 · $d2) / ($d1 · $n2) = $resNum / $resDen")
            }
            else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Unknown operation")
        }

        if (resDen < 0) {
            resNum = -resNum
            resDen = -resDen
        }

        val gcf = gcd(abs(resNum), abs(resDen))
        val simpNum = resNum / gcf
        val simpDen = resDen / gcf
        if (gcf > 1) {
            steps.add("Simplify by dividing numerator and denominator by GCF ($gcf): $simpNum / $simpDen")
        }

        val decimal = simpNum.toDouble() / simpDen.toDouble()
        val mixed = if (abs(simpNum) >= simpDen && simpDen != 1L) {
            val whole = simpNum / simpDen
            val rem = abs(simpNum % simpDen)
            if (rem == 0L) "$whole" else "$whole $rem/$simpDen"
        } else null

        val primary = if (simpDen == 1L) "$simpNum" else "$simpNum/$simpDen"
        val details = mutableListOf(
            "Simplified Fraction" to primary,
            "Decimal Value" to String.format("%.6f", decimal).trimEnd('0').trimEnd('.')
        )
        if (mixed != null) {
            details.add("Mixed Number" to mixed)
        }

        return CalculationResult(
            primaryResult = primary,
            details = details,
            steps = steps
        )
    }

    // 2. Percentage Calculator
    // Modes:
    // 1: What is X% of Y?
    // 2: X is what percent of Y?
    // 3: Percentage change from X to Y
    fun calculatePercentage(
        mode: Int,
        x: Double,
        y: Double
    ): CalculationResult {
        val steps = mutableListOf<String>()
        return when (mode) {
            1 -> {
                // What is X% of Y?
                val res = (x / 100.0) * y
                steps.add("Formula: Result = (X / 100) · Y")
                steps.add("Calculation: ($x / 100) · $y = ${x / 100.0} · $y = $res")
                CalculationResult(
                    primaryResult = formatNumber(res),
                    details = listOf(
                        "Calculation" to "$x% of $y",
                        "Result" to formatNumber(res)
                    ),
                    steps = steps
                )
            }
            2 -> {
                // X is what percent of Y?
                if (y == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Total (Y) cannot be zero")
                val pct = (x / y) * 100.0
                steps.add("Formula: Percentage = (X / Y) · 100%")
                steps.add("Calculation: ($x / $y) · 100% = ${x / y} · 100% = $pct%")
                CalculationResult(
                    primaryResult = "${formatNumber(pct)}%",
                    details = listOf(
                        "Ratio" to "$x / $y",
                        "Percentage" to "${formatNumber(pct)}%"
                    ),
                    steps = steps
                )
            }
            3 -> {
                // Percentage change from X to Y
                if (x == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Initial value (X) cannot be zero")
                val diff = y - x
                val pct = (diff / abs(x)) * 100.0
                val type = if (pct >= 0) "Increase" else "Decrease"
                steps.add("Formula: Percentage Change = ((Y - X) / |X|) · 100%")
                steps.add("Difference: $y - $x = $diff")
                steps.add("Percentage: ($diff / ${abs(x)}) · 100% = $pct%")
                CalculationResult(
                    primaryResult = "${formatNumber(abs(pct))}% $type",
                    details = listOf(
                        "Absolute Change" to formatNumber(diff),
                        "Percentage Change" to "${formatNumber(pct)}%",
                        "Direction" to type
                    ),
                    steps = steps
                )
            }
            else -> CalculationResult("Error", isSuccess = false, errorMessage = "Invalid percentage mode")
        }
    }

    // 3. Percent Error Calculator
    fun calculatePercentError(experimental: Double, theoretical: Double): CalculationResult {
        if (theoretical == 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Theoretical value cannot be zero")
        }
        val diff = experimental - theoretical
        val absDiff = abs(diff)
        val errorPct = (absDiff / abs(theoretical)) * 100.0
        val steps = listOf(
            "Formula: Percent Error = (|Experimental - Theoretical| / |Theoretical|) · 100%",
            "Step 1: Difference = |$experimental - $theoretical| = |$diff| = $absDiff",
            "Step 2: Divide by |Theoretical| = $absDiff / ${abs(theoretical)} = ${absDiff / abs(theoretical)}",
            "Step 3: Multiply by 100% = $errorPct%"
        )
        return CalculationResult(
            primaryResult = "${formatNumber(errorPct)}%",
            details = listOf(
                "Absolute Error" to formatNumber(absDiff),
                "Relative Error" to formatNumber(absDiff / abs(theoretical)),
                "Percent Error" to "${formatNumber(errorPct)}%"
            ),
            steps = steps
        )
    }

    // 4. Quadratic Formula Calculator: ax^2 + bx + c = 0
    fun calculateQuadratic(a: Double, b: Double, c: Double): CalculationResult {
        if (a == 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "'a' cannot be zero in a quadratic equation")
        }
        val discriminant = b * b - 4 * a * c
        val vertexX = -b / (2 * a)
        val vertexY = c - (b * b) / (4 * a)
        val steps = mutableListOf<String>()
        steps.add("Equation: ${a}x² + ${b}x + $c = 0")
        steps.add("Formula: x = (-b ± √(b² - 4ac)) / (2a)")
        steps.add("Discriminant Δ = b² - 4ac = ($b)² - 4($a)($c) = ${b*b} - ${4*a*c} = $discriminant")

        val details = mutableListOf<Pair<String, String>>()
        details.add("Discriminant (Δ)" to formatNumber(discriminant))
        details.add("Vertex (h, k)" to "(${formatNumber(vertexX)}, ${formatNumber(vertexY)})")
        details.add("Axis of Symmetry" to "x = ${formatNumber(vertexX)}")

        val primaryResult: String
        if (discriminant > 0) {
            val root1 = (-b + sqrt(discriminant)) / (2 * a)
            val root2 = (-b - sqrt(discriminant)) / (2 * a)
            steps.add("Since Δ > 0, there are two distinct real roots:")
            steps.add("x₁ = (-$b + √$discriminant) / (2·$a) = ${formatNumber(root1)}")
            steps.add("x₂ = (-$b - √$discriminant) / (2·$a) = ${formatNumber(root2)}")
            primaryResult = "x₁ = ${formatNumber(root1)}, x₂ = ${formatNumber(root2)}"
            details.add("Root 1 (x₁)" to formatNumber(root1))
            details.add("Root 2 (x₂)" to formatNumber(root2))
        } else if (abs(discriminant) < 1e-12) {
            val root = -b / (2 * a)
            steps.add("Since Δ = 0, there is exactly one repeated real root:")
            steps.add("x = -$b / (2·$a) = ${formatNumber(root)}")
            primaryResult = "x = ${formatNumber(root)}"
            details.add("Double Root (x)" to formatNumber(root))
        } else {
            val realPart = -b / (2 * a)
            val imagPart = sqrt(-discriminant) / (2 * a)
            steps.add("Since Δ < 0, there are two complex conjugate roots:")
            steps.add("x = ${formatNumber(realPart)} ± ${formatNumber(abs(imagPart))}i")
            primaryResult = "x = ${formatNumber(realPart)} ± ${formatNumber(abs(imagPart))}i"
            details.add("Complex Roots" to primaryResult)
        }

        return CalculationResult(
            primaryResult = primaryResult,
            details = details,
            steps = steps
        )
    }

    // 5. Exponent Calculator: x^y
    fun calculateExponent(base: Double, exponent: Double): CalculationResult {
        val steps = mutableListOf<String>()
        steps.add("Expression: ($base)^($exponent)")
        if (base == 0.0 && exponent <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "0 raised to non-positive power is undefined")
        }
        val result = base.pow(exponent)
        if (!result.isFinite()) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Result out of range or undefined")
        }
        steps.add("Evaluation: $base ^ $exponent = $result")
        return CalculationResult(
            primaryResult = formatNumber(result),
            details = listOf(
                "Base" to "$base",
                "Exponent" to "$exponent",
                "Result" to formatNumber(result)
            ),
            steps = steps
        )
    }

    // 6. Root Calculator: nth root of x
    fun calculateRoot(x: Double, n: Double): CalculationResult {
        if (n == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Degree (n) cannot be zero")
        if (x < 0.0 && n.toLong() % 2L == 0L) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Even root of negative number is complex")
        }
        val steps = mutableListOf<String>()
        steps.add("Formula: ⁿ√x = x^(1/n)")
        val res = if (x < 0) -((-x).pow(1.0 / n)) else x.pow(1.0 / n)
        steps.add("Computation: ($x)^(1/$n) = $res")
        return CalculationResult(
            primaryResult = formatNumber(res),
            details = listOf(
                "Radicand (x)" to "$x",
                "Degree (n)" to "$n",
                "Value" to formatNumber(res)
            ),
            steps = steps
        )
    }

    // 7. Log Calculator: log_base(x)
    fun calculateLog(x: Double, base: Double): CalculationResult {
        if (x <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Argument (x) must be greater than zero")
        if (base <= 0.0 || base == 1.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Base must be > 0 and ≠ 1")

        val result = ln(x) / ln(base)
        val steps = listOf(
            "Change of base formula: log_b(x) = ln(x) / ln(b)",
            "ln($x) = ${ln(x)}",
            "ln($base) = ${ln(base)}",
            "log_$base($x) = ${ln(x)} / ${ln(base)} = $result"
        )
        return CalculationResult(
            primaryResult = formatNumber(result),
            details = listOf(
                "Base $base Log" to formatNumber(result),
                "Natural Log ln(x)" to formatNumber(ln(x)),
                "Common Log log10(x)" to formatNumber(log10(x))
            ),
            steps = steps
        )
    }

    // 8. Ratio Calculator: A : B = C : D, solve for missing value
    fun calculateRatio(a: Double?, b: Double?, c: Double?, d: Double?): CalculationResult {
        val steps = mutableListOf<String>()
        steps.add("Proportion: A / B = C / D  <=>  A · D = B · C")
        val details = mutableListOf<Pair<String, String>>()
        var primary = ""

        when {
            a == null && b != null && c != null && d != null -> {
                if (d == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "D cannot be zero")
                val sol = (b * c) / d
                steps.add("Solve for A: A = (B · C) / D = ($b · $c) / $d = $sol")
                primary = "A = ${formatNumber(sol)}"
                details.add("Missing value A" to formatNumber(sol))
            }
            b == null && a != null && c != null && d != null -> {
                if (c == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "C cannot be zero")
                val sol = (a * d) / c
                steps.add("Solve for B: B = (A · D) / C = ($a · $d) / $c = $sol")
                primary = "B = ${formatNumber(sol)}"
                details.add("Missing value B" to formatNumber(sol))
            }
            c == null && a != null && b != null && d != null -> {
                if (b == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "B cannot be zero")
                val sol = (a * d) / b
                steps.add("Solve for C: C = (A · D) / B = ($a · $d) / $b = $sol")
                primary = "C = ${formatNumber(sol)}"
                details.add("Missing value C" to formatNumber(sol))
            }
            d == null && a != null && b != null && c != null -> {
                if (a == 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "A cannot be zero")
                val sol = (b * c) / a
                steps.add("Solve for D: D = (B · C) / A = ($b · $c) / $a = $sol")
                primary = "D = ${formatNumber(sol)}"
                details.add("Missing value D" to formatNumber(sol))
            }
            else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Provide exactly 3 values to solve for the 4th")
        }

        return CalculationResult(
            primaryResult = primary,
            details = details,
            steps = steps
        )
    }

    // 9. LCM & GCF Calculator
    fun calculateLcmGcf(numbers: List<Long>): CalculationResult {
        val valid = numbers.filter { it != 0L }
        if (valid.size < 2) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Provide at least two non-zero integers")
        }
        var currentGcd = abs(valid[0])
        var currentLcm = abs(valid[0])
        val steps = mutableListOf<String>()
        steps.add("Input numbers: ${valid.joinToString(", ")}")

        for (i in 1 until valid.size) {
            val num = abs(valid[i])
            val prevGcd = currentGcd
            currentGcd = gcd(currentGcd, num)
            currentLcm = (currentLcm / gcd(currentLcm, num)) * num
            steps.add("Pair (${prevGcd}, $num) => GCD = $currentGcd, LCM = $currentLcm")
        }

        return CalculationResult(
            primaryResult = "GCF = $currentGcd, LCM = $currentLcm",
            details = listOf(
                "Greatest Common Factor (GCF/GCD)" to "$currentGcd",
                "Least Common Multiple (LCM)" to "$currentLcm"
            ),
            steps = steps
        )
    }

    // 10. Factor Calculator
    fun calculateFactors(n: Long): CalculationResult {
        if (n == 0L) return CalculationResult("Error", isSuccess = false, errorMessage = "Every non-zero integer divides 0")
        val absN = abs(n)
        val factors = mutableListOf<Long>()
        val primeFactors = mutableListOf<Long>()

        for (i in 1..sqrt(absN.toDouble()).toLong()) {
            if (absN % i == 0L) {
                factors.add(i)
                if (i * i != absN) factors.add(absN / i)
            }
        }
        factors.sort()

        // Prime factorization
        var temp = absN
        var d = 2L
        while (d * d <= temp) {
            while (temp % d == 0L) {
                primeFactors.add(d)
                temp /= d
            }
            d++
        }
        if (temp > 1L) primeFactors.add(temp)

        val isPrime = absN > 1L && factors.size == 2
        val primeSummary = primeFactors.groupBy { it }
            .map { (prime, list) -> if (list.size > 1) "$prime^${list.size}" else "$prime" }
            .joinToString(" × ")

        val steps = listOf(
            "Number: $absN",
            "Factors tested up to √$absN ≈ ${sqrt(absN.toDouble()).toInt()}",
            "Prime Factorization: $primeSummary"
        )

        return CalculationResult(
            primaryResult = factors.joinToString(", "),
            details = listOf(
                "Total Factors" to "${factors.size}",
                "Prime?" to if (isPrime) "Yes (Prime Number)" else "No (Composite Number)",
                "Prime Factorization" to if (primeSummary.isEmpty()) "$absN" else primeSummary
            ),
            steps = steps
        )
    }

    // 11. Rounding Calculator
    fun calculateRounding(value: Double, places: Int, mode: String): CalculationResult {
        val bd = BigDecimal.valueOf(value)
        val rm = when (mode) {
            "Up (Ceil)" -> RoundingMode.CEILING
            "Down (Floor)" -> RoundingMode.FLOOR
            "Half Up" -> RoundingMode.HALF_UP
            "Half Even (Banker's)" -> RoundingMode.HALF_EVEN
            else -> RoundingMode.HALF_UP
        }
        val rounded = bd.setScale(places, rm)
        val steps = listOf(
            "Original Value: $value",
            "Target Decimal Places: $places",
            "Rounding Mode: $mode",
            "Result: $rounded"
        )
        return CalculationResult(
            primaryResult = rounded.toPlainString(),
            details = listOf(
                "Rounded Value" to rounded.toPlainString(),
                "Nearest Integer" to bd.setScale(0, rm).toPlainString()
            ),
            steps = steps
        )
    }

    // 12. Matrix Calculator (2x2 and 3x3 Determinant, Inversion, Addition, Multiplication)
    fun calculateMatrix2x2(
        a11: Double, a12: Double,
        a21: Double, a22: Double
    ): CalculationResult {
        val det = a11 * a22 - a12 * a21
        val steps = mutableListOf<String>()
        steps.add("Matrix: | $a11  $a12 |")
        steps.add("        | $a21  $a22 |")
        steps.add("Determinant = (a11 · a22) - (a12 · a21) = ($a11 · $a22) - ($a12 · $a21) = $det")

        val details = mutableListOf(
            "Determinant" to formatNumber(det),
            "Trace" to formatNumber(a11 + a22)
        )

        if (abs(det) > 1e-12) {
            val inv11 = a22 / det
            val inv12 = -a12 / det
            val inv21 = -a21 / det
            val inv22 = a11 / det
            val invStr = "[ ${formatNumber(inv11)}  ${formatNumber(inv12)} ; ${formatNumber(inv21)}  ${formatNumber(inv22)} ]"
            details.add("Inverse Matrix" to invStr)
            steps.add("Inverse formula: (1/det) · [ a22  -a12 ; -a21  a11 ]")
        } else {
            details.add("Inverse" to "Does not exist (Singular Matrix, det = 0)")
        }

        return CalculationResult(
            primaryResult = "Det = ${formatNumber(det)}",
            details = details,
            steps = steps
        )
    }

    // 13. Scientific Notation Calculator
    fun calculateScientificNotation(value: Double): CalculationResult {
        if (!value.isFinite()) return CalculationResult("Error", isSuccess = false, errorMessage = "Number must be finite")
        val sci = String.format(java.util.Locale.US, "%.6e", value)
        val parts = sci.split("e")
        val mantissa = parts[0].trimEnd('0').trimEnd('.')
        val exp = parts[1].toInt()
        val standardSci = "$mantissa × 10^$exp"
        val engineeringExp = (exp / 3) * 3
        val engMantissa = (mantissa.toDouble() * 10.0.pow((exp - engineeringExp).toDouble()))
        val engSci = "${formatNumber(engMantissa)} × 10^$engineeringExp"

        val steps = listOf(
            "Standard Form: $value",
            "Scientific Notation: $standardSci",
            "Engineering Notation: $engSci"
        )
        return CalculationResult(
            primaryResult = standardSci,
            details = listOf(
                "Scientific Form" to standardSci,
                "Engineering Form" to engSci,
                "E-Notation" to "${mantissa}E$exp"
            ),
            steps = steps
        )
    }

    // 14. Big Number Calculator
    fun calculateBigNumber(aStr: String, bStr: String, op: String): CalculationResult {
        return try {
            val a = BigInteger(aStr.trim())
            val b = BigInteger(bStr.trim())
            val steps = mutableListOf<String>()
            val res: BigInteger
            when (op) {
                "+" -> {
                    res = a + b
                    steps.add("Big Number Addition: A + B")
                }
                "-" -> {
                    res = a - b
                    steps.add("Big Number Subtraction: A - B")
                }
                "*" -> {
                    res = a * b
                    steps.add("Big Number Multiplication: A × B")
                }
                "/" -> {
                    if (b == BigInteger.ZERO) return CalculationResult("Error", isSuccess = false, errorMessage = "Division by zero")
                    res = a / b
                    val rem = a % b
                    steps.add("Big Number Division: A ÷ B = $res with remainder $rem")
                }
                "%" -> {
                    if (b == BigInteger.ZERO) return CalculationResult("Error", isSuccess = false, errorMessage = "Modulo by zero")
                    res = a % b
                    steps.add("Big Number Modulo: A mod B = $res")
                }
                else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Unsupported operation")
            }
            CalculationResult(
                primaryResult = res.toString(),
                details = listOf(
                    "Digit Count" to "${res.toString().length} digits",
                    "Sign" to if (res.signum() >= 0) "Positive" else "Negative"
                ),
                steps = steps
            )
        } catch (e: Exception) {
            CalculationResult("Error", isSuccess = false, errorMessage = "Invalid integer format: ${e.message}")
        }
    }

    // 15. Random Number Generator
    fun generateRandom(min: Long, max: Long, count: Int = 1, unique: Boolean = false): CalculationResult {
        if (min > max) return CalculationResult("Error", isSuccess = false, errorMessage = "Min cannot exceed Max")
        val range = max - min + 1
        if (unique && count > range) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Cannot pick $count unique values from a range of $range")
        }

        val results = mutableListOf<Long>()
        if (unique) {
            val pool = (min..max).toMutableList()
            pool.shuffle()
            results.addAll(pool.take(count))
        } else {
            repeat(count) {
                results.add(Random.nextLong(min, max + 1))
            }
        }

        return CalculationResult(
            primaryResult = results.joinToString(", "),
            details = listOf(
                "Count" to "${results.size}",
                "Range" to "[$min, $max]",
                "Average" to if (results.isNotEmpty()) formatNumber(results.average()) else "0"
            ),
            steps = listOf("Generated ${results.size} random numbers in range [$min, $max]")
        )
    }

    // 16. Binary & Hex Calculator
    fun calculateBases(input: String, fromBase: Int): CalculationResult {
        return try {
            val cleaned = input.trim()
            val dec = BigInteger(cleaned, fromBase)
            val bin = dec.toString(2)
            val oct = dec.toString(8)
            val hex = dec.toString(16).uppercase()

            val steps = listOf(
                "Input: $cleaned (Base $fromBase)",
                "Decimal Value: $dec",
                "Binary: $bin",
                "Hexadecimal: $hex"
            )
            CalculationResult(
                primaryResult = "Dec: $dec | Hex: $hex",
                details = listOf(
                    "Decimal (Base 10)" to dec.toString(),
                    "Binary (Base 2)" to bin,
                    "Hexadecimal (Base 16)" to hex,
                    "Octal (Base 8)" to oct
                ),
                steps = steps
            )
        } catch (e: Exception) {
            CalculationResult("Error", isSuccess = false, errorMessage = "Invalid number for base $fromBase")
        }
    }

    // 17. Half-Life Calculator: N(t) = N0 * (1/2)^(t / t_half)
    fun calculateHalfLife(
        initial: Double?,
        remaining: Double?,
        halfLife: Double?,
        time: Double?
    ): CalculationResult {
        val steps = mutableListOf<String>()
        steps.add("Formula: N(t) = N₀ · (1/2)^(t / t½)")
        val details = mutableListOf<Pair<String, String>>()
        var primary = ""

        when {
            // Solve for remaining N(t)
            remaining == null && initial != null && halfLife != null && time != null -> {
                if (halfLife <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Half-life must be > 0")
                val n = initial * 0.5.pow(time / halfLife)
                steps.add("Calculate N(t) = $initial · (0.5)^($time / $halfLife) = $n")
                primary = "Remaining N(t) = ${formatNumber(n)}"
                details.add("Remaining Amount" to formatNumber(n))
                details.add("Decayed Amount" to formatNumber(initial - n))
            }
            // Solve for initial N0
            initial == null && remaining != null && halfLife != null && time != null -> {
                if (halfLife <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Half-life must be > 0")
                val n0 = remaining / 0.5.pow(time / halfLife)
                steps.add("Calculate N₀ = N(t) / (0.5)^(t / t½) = $remaining / 0.5^($time / $halfLife) = $n0")
                primary = "Initial N₀ = ${formatNumber(n0)}"
                details.add("Initial Amount" to formatNumber(n0))
            }
            // Solve for halfLife t½
            halfLife == null && initial != null && remaining != null && time != null -> {
                if (remaining <= 0.0 || initial <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Quantities must be > 0")
                val hl = time / (ln(initial / remaining) / ln(2.0))
                steps.add("Calculate t½ = t / log₂(N₀ / N(t)) = $hl")
                primary = "Half-Life t½ = ${formatNumber(hl)}"
                details.add("Half-Life" to formatNumber(hl))
            }
            // Solve for elapsed time t
            time == null && initial != null && remaining != null && halfLife != null -> {
                if (remaining <= 0.0 || initial <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Quantities must be > 0")
                val t = halfLife * (ln(initial / remaining) / ln(2.0))
                steps.add("Calculate t = t½ · log₂(N₀ / N(t)) = $t")
                primary = "Elapsed Time t = ${formatNumber(t)}"
                details.add("Elapsed Time" to formatNumber(t))
            }
            else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Provide 3 values to solve for the 4th")
        }

        return CalculationResult(
            primaryResult = primary,
            details = details,
            steps = steps
        )
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = a
        var y = b
        while (y != 0L) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    fun formatNumber(value: Double): String {
        if (!value.isFinite()) return value.toString()
        if (abs(value - value.roundToLong()) < 1e-9) {
            return value.roundToLong().toString()
        }
        val formatted = String.format(java.util.Locale.US, "%.6f", value)
        return formatted.trimEnd('0').trimEnd('.')
    }
}
