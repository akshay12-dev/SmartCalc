package com.allcalc.app.domain.specialized

import kotlin.math.*

object GeometryCalculators {

    // 1. Pythagorean Theorem: a^2 + b^2 = c^2
    fun calculatePythagorean(a: Double?, b: Double?, c: Double?): CalculationResult {
        val steps = mutableListOf<String>()
        val details = mutableListOf<Pair<String, String>>()
        val primary: String

        when {
            a != null && b != null && c == null -> {
                if (a <= 0.0 || b <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Sides must be positive")
                val hypotenuse = sqrt(a * a + b * b)
                steps.add("Formula: c = √(a² + b²)")
                steps.add("c = √($a² + $b²) = √(${a * a} + ${b * b}) = √${a * a + b * b} = ${MathCalculators.formatNumber(hypotenuse)}")
                primary = "c = ${MathCalculators.formatNumber(hypotenuse)}"
                details.add("Hypotenuse (c)" to MathCalculators.formatNumber(hypotenuse))
                details.add("Area" to MathCalculators.formatNumber(0.5 * a * b))
                details.add("Perimeter" to MathCalculators.formatNumber(a + b + hypotenuse))
            }
            a != null && c != null && b == null -> {
                if (c <= a) return CalculationResult("Error", isSuccess = false, errorMessage = "Hypotenuse c must be greater than leg a")
                val legB = sqrt(c * c - a * a)
                steps.add("Formula: b = √(c² - a²)")
                steps.add("b = √($c² - $a²) = √(${c * c} - ${a * a}) = √${c * c - a * a} = ${MathCalculators.formatNumber(legB)}")
                primary = "b = ${MathCalculators.formatNumber(legB)}"
                details.add("Leg (b)" to MathCalculators.formatNumber(legB))
                details.add("Area" to MathCalculators.formatNumber(0.5 * a * legB))
                details.add("Perimeter" to MathCalculators.formatNumber(a + legB + c))
            }
            b != null && c != null && a == null -> {
                if (c <= b) return CalculationResult("Error", isSuccess = false, errorMessage = "Hypotenuse c must be greater than leg b")
                val legA = sqrt(c * c - b * b)
                steps.add("Formula: a = √(c² - b²)")
                steps.add("a = √($c² - $b²) = √(${c * c} - ${b * b}) = √${c * c - b * b} = ${MathCalculators.formatNumber(legA)}")
                primary = "a = ${MathCalculators.formatNumber(legA)}"
                details.add("Leg (a)" to MathCalculators.formatNumber(legA))
                details.add("Area" to MathCalculators.formatNumber(0.5 * legA * b))
                details.add("Perimeter" to MathCalculators.formatNumber(legA + b + c))
            }
            else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Provide exactly two values to find the third")
        }

        return CalculationResult(
            primaryResult = primary,
            details = details,
            steps = steps
        )
    }

    // 2. Right Triangle Calculator
    fun calculateRightTriangle(
        legA: Double?,
        legB: Double?,
        hypotenuseC: Double?,
        angleAdeg: Double?
    ): CalculationResult {
        var a = legA
        var b = legB
        var c = hypotenuseC
        var alpha = angleAdeg

        val steps = mutableListOf<String>()
        val details = mutableListOf<Pair<String, String>>()

        when {
            // Given two legs
            a != null && b != null -> {
                c = hypot(a, b)
                alpha = Math.toDegrees(atan(a / b))
            }
            // Given leg and hypotenuse
            a != null && c != null -> {
                if (c <= a) return CalculationResult("Error", isSuccess = false, errorMessage = "Hypotenuse must be greater than leg a")
                b = sqrt(c * c - a * a)
                alpha = Math.toDegrees(asin(a / c))
            }
            b != null && c != null -> {
                if (c <= b) return CalculationResult("Error", isSuccess = false, errorMessage = "Hypotenuse must be greater than leg b")
                a = sqrt(c * c - b * b)
                alpha = Math.toDegrees(acos(b / c))
            }
            // Given leg and angle
            a != null && alpha != null -> {
                val rad = Math.toRadians(alpha)
                c = a / sin(rad)
                b = a / tan(rad)
            }
            c != null && alpha != null -> {
                val rad = Math.toRadians(alpha)
                a = c * sin(rad)
                b = c * cos(rad)
            }
            else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Provide at least two values (sides or angle)")
        }

        val beta = 90.0 - alpha!!
        val area = 0.5 * a!! * b!!
        val perimeter = a + b + c!!

        steps.add("Sides: a = ${MathCalculators.formatNumber(a)}, b = ${MathCalculators.formatNumber(b)}, c = ${MathCalculators.formatNumber(c)}")
        steps.add("Angles: ∠A = ${MathCalculators.formatNumber(alpha)}°, ∠B = ${MathCalculators.formatNumber(beta)}°, ∠C = 90°")
        steps.add("Area = 1/2 · a · b = ${MathCalculators.formatNumber(area)}")
        steps.add("Perimeter = a + b + c = ${MathCalculators.formatNumber(perimeter)}")

        details.add("Side a (Leg)" to MathCalculators.formatNumber(a))
        details.add("Side b (Leg)" to MathCalculators.formatNumber(b))
        details.add("Side c (Hypotenuse)" to MathCalculators.formatNumber(c))
        details.add("Angle A" to "${MathCalculators.formatNumber(alpha)}°")
        details.add("Angle B" to "${MathCalculators.formatNumber(beta)}°")
        details.add("Area" to MathCalculators.formatNumber(area))
        details.add("Perimeter" to MathCalculators.formatNumber(perimeter))

        return CalculationResult(
            primaryResult = "Area: ${MathCalculators.formatNumber(area)} | c = ${MathCalculators.formatNumber(c)}",
            details = details,
            steps = steps
        )
    }

    // 3. General Triangle Calculator (Heron's Formula & Angles)
    fun calculateTriangle(a: Double, b: Double, c: Double): CalculationResult {
        if (a <= 0.0 || b <= 0.0 || c <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "All sides must be positive")
        }
        if (a + b <= c || a + c <= b || b + c <= a) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Triangle inequality violated: sum of any 2 sides must exceed 3rd side")
        }

        val s = (a + b + c) / 2.0
        val area = sqrt(s * (s - a) * (s - b) * (s - c))
        val perimeter = a + b + c

        // Angles via Law of Cosines
        val angleA = Math.toDegrees(acos(((b * b + c * c - a * a) / (2 * b * c)).coerceIn(-1.0, 1.0)))
        val angleB = Math.toDegrees(acos(((a * a + c * c - b * b) / (2 * a * c)).coerceIn(-1.0, 1.0)))
        val angleC = 180.0 - angleA - angleB

        val steps = listOf(
            "Semi-perimeter s = (a + b + c) / 2 = ($a + $b + $c) / 2 = $s",
            "Heron's Formula: Area = √(s(s - a)(s - b)(s - c)) = $area",
            "Law of Cosines: cos(A) = (b² + c² - a²) / (2bc) => ∠A = ${MathCalculators.formatNumber(angleA)}°",
            "Law of Cosines: cos(B) = (a² + c² - b²) / (2ac) => ∠B = ${MathCalculators.formatNumber(angleB)}°",
            "Angle C = 180° - ∠A - ∠B = ${MathCalculators.formatNumber(angleC)}°"
        )

        return CalculationResult(
            primaryResult = "Area: ${MathCalculators.formatNumber(area)}",
            details = listOf(
                "Area" to MathCalculators.formatNumber(area),
                "Perimeter" to MathCalculators.formatNumber(perimeter),
                "Semi-perimeter (s)" to MathCalculators.formatNumber(s),
                "Angle A" to "${MathCalculators.formatNumber(angleA)}°",
                "Angle B" to "${MathCalculators.formatNumber(angleB)}°",
                "Angle C" to "${MathCalculators.formatNumber(angleC)}°"
            ),
            steps = steps
        )
    }

    // 4. Circle Calculator
    fun calculateCircle(type: String, value: Double): CalculationResult {
        if (value <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Value must be positive")
        val radius = when (type.lowercase()) {
            "radius" -> value
            "diameter" -> value / 2.0
            "circumference" -> value / (2.0 * Math.PI)
            "area" -> sqrt(value / Math.PI)
            else -> value
        }

        val diameter = 2.0 * radius
        val circumference = 2.0 * Math.PI * radius
        val area = Math.PI * radius * radius

        val steps = listOf(
            "Given $type = $value",
            "Radius r = ${MathCalculators.formatNumber(radius)}",
            "Diameter d = 2r = ${MathCalculators.formatNumber(diameter)}",
            "Circumference C = 2πr = ${MathCalculators.formatNumber(circumference)}",
            "Area A = πr² = ${MathCalculators.formatNumber(area)}"
        )

        return CalculationResult(
            primaryResult = "Area = ${MathCalculators.formatNumber(area)}",
            details = listOf(
                "Radius (r)" to MathCalculators.formatNumber(radius),
                "Diameter (d)" to MathCalculators.formatNumber(diameter),
                "Circumference (C)" to MathCalculators.formatNumber(circumference),
                "Area (A)" to MathCalculators.formatNumber(area)
            ),
            steps = steps
        )
    }

    // 5. 2D Area Calculator for shapes
    fun calculateArea2D(shape: String, p1: Double, p2: Double? = null, p3: Double? = null): CalculationResult {
        val steps = mutableListOf<String>()
        val details = mutableListOf<Pair<String, String>>()
        val area: Double
        val perimeter: Double

        when (shape.lowercase()) {
            "rectangle" -> {
                val length = p1
                val width = p2 ?: 0.0
                if (length <= 0 || width <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Dimensions must be positive")
                area = length * width
                perimeter = 2 * (length + width)
                steps.add("Formula: Area = length · width = $length · $width = $area")
                steps.add("Perimeter = 2(length + width) = 2($length + $width) = $perimeter")
            }
            "square" -> {
                val side = p1
                if (side <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Side must be positive")
                area = side * side
                perimeter = 4 * side
                steps.add("Formula: Area = side² = $side² = $area")
                steps.add("Perimeter = 4 · side = $perimeter")
            }
            "trapezoid" -> {
                val a = p1
                val b = p2 ?: 0.0
                val h = p3 ?: 0.0
                if (a <= 0 || b <= 0 || h <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Bases and height must be positive")
                area = 0.5 * (a + b) * h
                perimeter = 0.0
                steps.add("Formula: Area = 1/2 · (a + b) · h = 0.5 · ($a + $b) · $h = $area")
            }
            "parallelogram" -> {
                val base = p1
                val height = p2 ?: 0.0
                if (base <= 0 || height <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Base and height must be positive")
                area = base * height
                perimeter = 0.0
                steps.add("Formula: Area = base · height = $base · $height = $area")
            }
            "ellipse" -> {
                val a = p1
                val b = p2 ?: 0.0
                if (a <= 0 || b <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Axes must be positive")
                area = Math.PI * a * b
                // Ramanujan approximation for ellipse perimeter
                perimeter = Math.PI * (3 * (a + b) - sqrt((3 * a + b) * (a + 3 * b)))
                steps.add("Formula: Area = π · a · b = π · $a · $b = $area")
                steps.add("Perimeter ≈ π [ 3(a+b) - √((3a+b)(a+3b)) ] = $perimeter")
            }
            else -> return calculateCircle("radius", p1)
        }

        details.add("Area" to MathCalculators.formatNumber(area))
        if (perimeter > 0) details.add("Perimeter" to MathCalculators.formatNumber(perimeter))

        return CalculationResult(
            primaryResult = "Area: ${MathCalculators.formatNumber(area)}",
            details = details,
            steps = steps
        )
    }

    // 6. Volume & Surface Area Calculator (3D Solids)
    fun calculateSolid3D(solid: String, p1: Double, p2: Double? = null, p3: Double? = null): CalculationResult {
        val steps = mutableListOf<String>()
        val details = mutableListOf<Pair<String, String>>()
        val volume: Double
        val surfaceArea: Double

        when (solid.lowercase()) {
            "cube" -> {
                val a = p1
                if (a <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Edge length must be positive")
                volume = a * a * a
                surfaceArea = 6 * a * a
                steps.add("Volume = a³ = $a³ = $volume")
                steps.add("Surface Area = 6a² = 6 · $a² = $surfaceArea")
            }
            "sphere" -> {
                val r = p1
                if (r <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Radius must be positive")
                volume = (4.0 / 3.0) * Math.PI * r.pow(3)
                surfaceArea = 4.0 * Math.PI * r * r
                steps.add("Volume = (4/3)πr³ = (4/3) · π · $r³ = $volume")
                steps.add("Surface Area = 4πr² = 4 · π · $r² = $surfaceArea")
            }
            "cylinder" -> {
                val r = p1
                val h = p2 ?: 0.0
                if (r <= 0 || h <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Radius and height must be positive")
                volume = Math.PI * r * r * h
                surfaceArea = 2.0 * Math.PI * r * (r + h)
                steps.add("Volume = πr²h = π · $r² · $h = $volume")
                steps.add("Surface Area = 2πr(r + h) = 2π · $r($r + $h) = $surfaceArea")
            }
            "cone" -> {
                val r = p1
                val h = p2 ?: 0.0
                if (r <= 0 || h <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Radius and height must be positive")
                val slant = sqrt(r * r + h * h)
                volume = (1.0 / 3.0) * Math.PI * r * r * h
                surfaceArea = Math.PI * r * (r + slant)
                steps.add("Slant height l = √(r² + h²) = $slant")
                steps.add("Volume = (1/3)πr²h = $volume")
                steps.add("Surface Area = πr(r + l) = $surfaceArea")
            }
            "rectangular_prism" -> {
                val l = p1
                val w = p2 ?: 0.0
                val h = p3 ?: 0.0
                if (l <= 0 || w <= 0 || h <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Dimensions must be positive")
                volume = l * w * h
                surfaceArea = 2 * (l * w + l * h + w * h)
                steps.add("Volume = length · width · height = $l · $w · $h = $volume")
                steps.add("Surface Area = 2(lw + lh + wh) = $surfaceArea")
            }
            else -> return CalculationResult("Error", isSuccess = false, errorMessage = "Unsupported 3D solid")
        }

        details.add("Volume" to MathCalculators.formatNumber(volume))
        details.add("Surface Area" to MathCalculators.formatNumber(surfaceArea))

        return CalculationResult(
            primaryResult = "Volume: ${MathCalculators.formatNumber(volume)} | Area: ${MathCalculators.formatNumber(surfaceArea)}",
            details = details,
            steps = steps
        )
    }

    // 7. Slope Calculator: (x1, y1) to (x2, y2)
    fun calculateSlope(x1: Double, y1: Double, x2: Double, y2: Double): CalculationResult {
        val dx = x2 - x1
        val dy = y2 - y1
        val steps = mutableListOf<String>()
        val details = mutableListOf<Pair<String, String>>()

        val dist = sqrt(dx * dx + dy * dy)
        val midX = (x1 + x2) / 2.0
        val midY = (y1 + y2) / 2.0

        steps.add("Points: ($x1, $y1) and ($x2, $y2)")
        steps.add("Δx = $x2 - $x1 = $dx, Δy = $y2 - $y1 = $dy")

        val primary: String
        if (abs(dx) < 1e-12) {
            primary = "Slope: Undefined (Vertical Line)"
            details.add("Slope (m)" to "Undefined")
            details.add("Equation" to "x = $x1")
            steps.add("Slope m = Δy / 0 => Line is vertical")
        } else {
            val m = dy / dx
            val b = y1 - m * x1
            val angleRad = atan(m)
            val angleDeg = Math.toDegrees(angleRad)
            val bStr = if (b >= 0) "+ ${MathCalculators.formatNumber(b)}" else "- ${MathCalculators.formatNumber(abs(b))}"
            val eq = "y = ${MathCalculators.formatNumber(m)}x $bStr"
            primary = "Slope m = ${MathCalculators.formatNumber(m)}"
            details.add("Slope (m)" to MathCalculators.formatNumber(m))
            details.add("Line Equation" to eq)
            details.add("Angle of Inclination" to "${MathCalculators.formatNumber(angleDeg)}°")
            steps.add("Slope m = Δy / Δx = $dy / $dx = ${MathCalculators.formatNumber(m)}")
            steps.add("Y-intercept b = y₁ - m·x₁ = $y1 - (${MathCalculators.formatNumber(m)}·$x1) = ${MathCalculators.formatNumber(b)}")
        }

        details.add("Midpoint" to "(${MathCalculators.formatNumber(midX)}, ${MathCalculators.formatNumber(midY)})")
        details.add("Distance" to MathCalculators.formatNumber(dist))
        steps.add("Midpoint = ((x₁+x₂)/2, (y₁+y₂)/2) = (${MathCalculators.formatNumber(midX)}, ${MathCalculators.formatNumber(midY)})")
        steps.add("Distance d = √(Δx² + Δy²) = ${MathCalculators.formatNumber(dist)}")

        return CalculationResult(
            primaryResult = primary,
            details = details,
            steps = steps
        )
    }

    // 8. Distance Calculator (2D and 3D)
    fun calculateDistance(
        x1: Double, y1: Double, z1: Double?,
        x2: Double, y2: Double, z2: Double?
    ): CalculationResult {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = if (z1 != null && z2 != null) z2 - z1 else 0.0

        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        val is3D = z1 != null && z2 != null

        val steps = if (is3D) {
            listOf(
                "3D Distance Formula: d = √((x₂ - x₁)² + (y₂ - y₁)² + (z₂ - z₁)²)",
                "d = √(($x2 - $x1)² + ($y2 - $y1)² + ($z2 - $z1)²)",
                "d = √(${dx*dx} + ${dy*dy} + ${dz*dz}) = √${dx*dx + dy*dy + dz*dz} = ${MathCalculators.formatNumber(dist)}"
            )
        } else {
            listOf(
                "2D Distance Formula: d = √((x₂ - x₁)² + (y₂ - y₁)²)",
                "d = √(($x2 - $x1)² + ($y2 - $y1)²)",
                "d = √(${dx*dx} + ${dy*dy}) = √${dx*dx + dy*dy} = ${MathCalculators.formatNumber(dist)}"
            )
        }

        return CalculationResult(
            primaryResult = "Distance = ${MathCalculators.formatNumber(dist)}",
            details = listOf(
                "Distance" to MathCalculators.formatNumber(dist),
                "Δx" to MathCalculators.formatNumber(abs(dx)),
                "Δy" to MathCalculators.formatNumber(abs(dy))
            ) + (if (is3D) listOf("Δz" to MathCalculators.formatNumber(abs(dz))) else emptyList()),
            steps = steps
        )
    }
}
