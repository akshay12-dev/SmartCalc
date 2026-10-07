package com.allcalc.app.domain.specialized

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class SpecializedCategory(val title: String) {
    ALL("All"),
    MATH("Math"),
    STATISTICS("Statistics"),
    GEOMETRY("Geometry"),
    FINANCIAL("Financial"),
    FITNESS("Fitness & Health"),
    OTHER("Other")
}

data class SpecializedCalculatorDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: SpecializedCategory,
    val keywords: List<String>,
    val iconName: String = "Calculate"
) {
    fun getIcon(): ImageVector {
        return when (iconName) {
            "Percent" -> Icons.Filled.Percent
            "PieChart" -> Icons.Filled.PieChart
            "Casino" -> Icons.Filled.Casino
            "Functions" -> Icons.Filled.Functions
            "DataObject" -> Icons.Filled.DataObject
            "BarChart" -> Icons.Filled.BarChart
            "ShowChart" -> Icons.Filled.Timeline
            "Architecture" -> Icons.Filled.Architecture
            "Category" -> Icons.Filled.Category
            "Straighten" -> Icons.Filled.Straighten
            "Timeline" -> Icons.Filled.Timeline
            "Numbers" -> Icons.Filled.Numbers
            "Science" -> Icons.Filled.Science
            "AttachMoney" -> Icons.Filled.AttachMoney
            "FitnessCenter" -> Icons.Filled.FitnessCenter
            "DateRange" -> Icons.Filled.DateRange
            "School" -> Icons.Filled.School
            else -> Icons.Filled.Calculate
        }
    }
}

object SpecializedCatalog {

    val allCalculators: List<SpecializedCalculatorDef> = listOf(
        // --- MATH CALCULATORS ---
        SpecializedCalculatorDef(
            id = "fraction",
            title = "Fraction Calculator",
            subtitle = "Add, subtract, multiply, divide & simplify fractions",
            category = SpecializedCategory.MATH,
            keywords = listOf("fraction", "numerator", "denominator", "simplify", "mixed number", "divide", "add"),
            iconName = "Functions"
        ),
        SpecializedCalculatorDef(
            id = "percentage",
            title = "Percentage Calculator",
            subtitle = "X% of Y, percentage of total, and % increase/decrease",
            category = SpecializedCategory.MATH,
            keywords = listOf("percentage", "percent", "%", "discount", "increase", "decrease", "change"),
            iconName = "Percent"
        ),
        SpecializedCalculatorDef(
            id = "percent_error",
            title = "Percent Error Calculator",
            subtitle = "Compare experimental and theoretical values",
            category = SpecializedCategory.MATH,
            keywords = listOf("percent error", "experimental", "theoretical", "accuracy", "error"),
            iconName = "Percent"
        ),
        SpecializedCalculatorDef(
            id = "quadratic",
            title = "Quadratic Formula Calculator",
            subtitle = "Solve ax² + bx + c = 0 with real/complex roots and vertex",
            category = SpecializedCategory.MATH,
            keywords = listOf("quadratic", "parabola", "roots", "discriminant", "vertex", "algebra"),
            iconName = "Functions"
        ),
        SpecializedCalculatorDef(
            id = "exponent",
            title = "Exponent Calculator",
            subtitle = "Powers, negative exponents, fractional powers (x^y)",
            category = SpecializedCategory.MATH,
            keywords = listOf("exponent", "power", "x^y", "superscript", "polynomial"),
            iconName = "Functions"
        ),
        SpecializedCalculatorDef(
            id = "root",
            title = "Root Calculator",
            subtitle = "Square root, cube root, and general nth root (ⁿ√x)",
            category = SpecializedCategory.MATH,
            keywords = listOf("root", "square root", "cube root", "radical", "nth root", "surd"),
            iconName = "Functions"
        ),
        SpecializedCalculatorDef(
            id = "log",
            title = "Log Calculator",
            subtitle = "Logarithms to any base, natural ln(x), and log10(x)",
            category = SpecializedCategory.MATH,
            keywords = listOf("log", "logarithm", "ln", "natural log", "base 10", "base 2"),
            iconName = "Functions"
        ),
        SpecializedCalculatorDef(
            id = "ratio",
            title = "Ratio Calculator",
            subtitle = "Solve proportions A : B = C : D and simplify ratios",
            category = SpecializedCategory.MATH,
            keywords = listOf("ratio", "proportion", "scale", "aspect ratio", "solve ratio"),
            iconName = "Category"
        ),
        SpecializedCalculatorDef(
            id = "lcm_gcf",
            title = "Least Common Multiple & GCF",
            subtitle = "Find LCM and Greatest Common Factor (GCD) of numbers",
            category = SpecializedCategory.MATH,
            keywords = listOf("lcm", "gcf", "gcd", "least common multiple", "greatest common factor", "divisor"),
            iconName = "Numbers"
        ),
        SpecializedCalculatorDef(
            id = "factor",
            title = "Factor Calculator",
            subtitle = "All integer divisors, prime factorization, and primality",
            category = SpecializedCategory.MATH,
            keywords = listOf("factor", "prime", "prime factorization", "divisors", "composite"),
            iconName = "Numbers"
        ),
        SpecializedCalculatorDef(
            id = "rounding",
            title = "Rounding Calculator",
            subtitle = "Round numbers to decimal places, ceiling, floor, half-even",
            category = SpecializedCategory.MATH,
            keywords = listOf("round", "rounding", "ceiling", "floor", "significant figures", "decimals"),
            iconName = "Numbers"
        ),
        SpecializedCalculatorDef(
            id = "matrix",
            title = "Matrix Calculator",
            subtitle = "2×2 matrix determinant, trace, and inverse matrix",
            category = SpecializedCategory.MATH,
            keywords = listOf("matrix", "determinant", "inverse", "linear algebra", "trace"),
            iconName = "DataObject"
        ),
        SpecializedCalculatorDef(
            id = "scientific_notation",
            title = "Scientific Notation Calculator",
            subtitle = "Convert to standard scientific & engineering notation (10^n)",
            category = SpecializedCategory.MATH,
            keywords = listOf("scientific notation", "standard form", "powers of 10", "engineering notation"),
            iconName = "Science"
        ),
        SpecializedCalculatorDef(
            id = "big_number",
            title = "Big Number Calculator",
            subtitle = "High-precision arbitrary digit arithmetic (+, -, ×, ÷, %)",
            category = SpecializedCategory.MATH,
            keywords = listOf("big number", "huge number", "arbitrary precision", "bigint", "large digits"),
            iconName = "Calculate"
        ),
        SpecializedCalculatorDef(
            id = "rng",
            title = "Random Number Generator",
            subtitle = "Generate random integers, non-repeating lists, or rolls",
            category = SpecializedCategory.MATH,
            keywords = listOf("random", "rng", "dice", "roll", "generator", "lottery", "chance"),
            iconName = "Casino"
        ),
        SpecializedCalculatorDef(
            id = "binary_hex",
            title = "Binary & Hex Calculator",
            subtitle = "Convert and calculate across Binary, Hex, Decimal, and Octal",
            category = SpecializedCategory.MATH,
            keywords = listOf("binary", "hex", "hexadecimal", "octal", "base 2", "base 16", "bitwise", "programmer"),
            iconName = "DataObject"
        ),
        SpecializedCalculatorDef(
            id = "half_life",
            title = "Half-Life Calculator",
            subtitle = "Radioactive decay, remaining quantity, initial amount & time",
            category = SpecializedCategory.MATH,
            keywords = listOf("half-life", "decay", "radioactive", "exponential decay", "physics", "chemistry"),
            iconName = "Science"
        ),

        // --- STATISTICS CALCULATORS ---
        SpecializedCalculatorDef(
            id = "descriptive_stats",
            title = "Statistics Calculator",
            subtitle = "Full dataset analysis: Mean, Median, Mode, Variance, Std Dev, IQR",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("statistics", "dataset", "summary", "variance", "standard deviation", "iqr"),
            iconName = "BarChart"
        ),
        SpecializedCalculatorDef(
            id = "std_dev",
            title = "Standard Deviation Calculator",
            subtitle = "Sample (s) and Population (σ) standard deviation & variance",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("standard deviation", "std dev", "variance", "sigma", "spread", "dispersion"),
            iconName = "ShowChart"
        ),
        SpecializedCalculatorDef(
            id = "mean_median_mode",
            title = "Mean, Median, Mode, Range",
            subtitle = "Central tendency and range of any set of values",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("mean", "median", "mode", "range", "average", "central tendency"),
            iconName = "BarChart"
        ),
        SpecializedCalculatorDef(
            id = "permutation_combination",
            title = "Permutation & Combination",
            subtitle = "nPr and nCr with or without repetition and full steps",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("permutation", "combination", "npr", "ncr", "factorial", "arrangements", "selection"),
            iconName = "Functions"
        ),
        SpecializedCalculatorDef(
            id = "probability",
            title = "Probability Calculator",
            subtitle = "Event probability, complement, odds for and against",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("probability", "chance", "odds", "outcomes", "likelihood"),
            iconName = "PieChart"
        ),
        SpecializedCalculatorDef(
            id = "z_score",
            title = "Z-Score Calculator",
            subtitle = "Standardized score Z = (X - μ) / σ and normal percentile",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("z-score", "standard score", "normal distribution", "percentile", "gaussian"),
            iconName = "ShowChart"
        ),
        SpecializedCalculatorDef(
            id = "confidence_interval",
            title = "Confidence Interval Calculator",
            subtitle = "Margin of error and confidence interval for 90%, 95%, 99%",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("confidence interval", "margin of error", "ci", "standard error", "z-interval"),
            iconName = "Timeline"
        ),
        SpecializedCalculatorDef(
            id = "sequence",
            title = "Number Sequence Calculator",
            subtitle = "Arithmetic & Geometric progression: n-th term and sum Sn",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("sequence", "arithmetic sequence", "geometric sequence", "progression", "series", "sum"),
            iconName = "Timeline"
        ),
        SpecializedCalculatorDef(
            id = "sample_size",
            title = "Sample Size Calculator",
            subtitle = "Cochran's formula for survey and experiment sample size",
            category = SpecializedCategory.STATISTICS,
            keywords = listOf("sample size", "survey", "cochran", "population", "margin of error"),
            iconName = "Numbers"
        ),

        // --- GEOMETRY CALCULATORS ---
        SpecializedCalculatorDef(
            id = "pythagorean",
            title = "Pythagorean Theorem",
            subtitle = "Solve a² + b² = c² for legs or hypotenuse",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("pythagorean", "pythagoras", "hypotenuse", "right triangle", "a^2+b^2=c^2"),
            iconName = "Architecture"
        ),
        SpecializedCalculatorDef(
            id = "right_triangle",
            title = "Right Triangle Calculator",
            subtitle = "Solve all sides, angles, area and perimeter from any 2 inputs",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("right triangle", "trigonometry", "sin cos tan", "hypotenuse", "legs"),
            iconName = "Architecture"
        ),
        SpecializedCalculatorDef(
            id = "triangle",
            title = "Triangle Calculator",
            subtitle = "Heron's formula, all 3 angles, area and perimeter",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("triangle", "heron", "scalene", "isosceles", "equilateral", "triangle area"),
            iconName = "Architecture"
        ),
        SpecializedCalculatorDef(
            id = "circle",
            title = "Circle Calculator",
            subtitle = "Calculate radius, diameter, circumference and area",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("circle", "radius", "diameter", "circumference", "pi", "perimeter"),
            iconName = "Category"
        ),
        SpecializedCalculatorDef(
            id = "area_2d",
            title = "Area Calculator",
            subtitle = "2D shapes: Rectangle, Square, Triangle, Circle, Trapezoid, Ellipse",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("area", "rectangle", "square", "trapezoid", "parallelogram", "ellipse", "2d area"),
            iconName = "Straighten"
        ),
        SpecializedCalculatorDef(
            id = "volume",
            title = "Volume Calculator",
            subtitle = "3D solids: Cube, Sphere, Cylinder, Cone, Rectangular Prism",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("volume", "cube", "sphere", "cylinder", "cone", "prism", "3d capacity"),
            iconName = "Category"
        ),
        SpecializedCalculatorDef(
            id = "surface_area",
            title = "Surface Area Calculator",
            subtitle = "Calculate total outer surface area of 3D solids",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("surface area", "total surface area", "cube", "cylinder", "sphere", "cone"),
            iconName = "Category"
        ),
        SpecializedCalculatorDef(
            id = "slope",
            title = "Slope Calculator",
            subtitle = "Slope m, inclination angle, y = mx + b, midpoint and length",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("slope", "gradient", "y=mx+b", "linear equation", "midpoint", "line"),
            iconName = "ShowChart"
        ),
        SpecializedCalculatorDef(
            id = "distance",
            title = "Distance Calculator",
            subtitle = "2D and 3D Euclidean distance between coordinates",
            category = SpecializedCategory.GEOMETRY,
            keywords = listOf("distance", "euclidean distance", "coordinates", "2d distance", "3d distance", "points"),
            iconName = "Straighten"
        ),

        // --- FINANCIAL CALCULATORS ---
        SpecializedCalculatorDef(
            id = "loan",
            title = "Loan & Mortgage Calculator",
            subtitle = "Calculate monthly EMI, total interest, and loan amortization",
            category = SpecializedCategory.FINANCIAL,
            keywords = listOf("loan", "mortgage", "emi", "interest", "debt", "amortization", "payment", "bank"),
            iconName = "AttachMoney"
        ),
        SpecializedCalculatorDef(
            id = "interest",
            title = "Interest Calculator",
            subtitle = "Simple and Compound interest with custom compounding frequencies",
            category = SpecializedCategory.FINANCIAL,
            keywords = listOf("interest", "compound interest", "simple interest", "savings", "yield", "rate"),
            iconName = "AttachMoney"
        ),
        SpecializedCalculatorDef(
            id = "tip",
            title = "Tip & Bill Split Calculator",
            subtitle = "Calculate tip percentage and split bills among multiple people",
            category = SpecializedCategory.FINANCIAL,
            keywords = listOf("tip", "bill split", "gratuity", "restaurant", "dining", "split"),
            iconName = "AttachMoney"
        ),
        SpecializedCalculatorDef(
            id = "investment",
            title = "Investment & ROI Calculator",
            subtitle = "Compound growth with monthly contributions and return on investment",
            category = SpecializedCategory.FINANCIAL,
            keywords = listOf("investment", "roi", "returns", "stocks", "savings", "wealth", "compound"),
            iconName = "AttachMoney"
        ),

        // --- FITNESS AND HEALTH CALCULATORS ---
        SpecializedCalculatorDef(
            id = "bmi",
            title = "BMI Calculator",
            subtitle = "Body Mass Index, category classification, and ideal weight range",
            category = SpecializedCategory.FITNESS,
            keywords = listOf("bmi", "body mass index", "weight", "height", "healthy weight", "fitness"),
            iconName = "FitnessCenter"
        ),
        SpecializedCalculatorDef(
            id = "calorie_bmr",
            title = "Calorie & BMR Calculator",
            subtitle = "Daily maintenance calories (TDEE) and Basal Metabolic Rate",
            category = SpecializedCategory.FITNESS,
            keywords = listOf("bmr", "calories", "tdee", "metabolism", "diet", "weight loss", "maintenance"),
            iconName = "FitnessCenter"
        ),
        SpecializedCalculatorDef(
            id = "body_fat",
            title = "Body Fat Calculator",
            subtitle = "Estimate body fat percentage using U.S. Navy circumference method",
            category = SpecializedCategory.FITNESS,
            keywords = listOf("body fat", "lean mass", "fat percentage", "navy", "circumference"),
            iconName = "FitnessCenter"
        ),

        // --- OTHER CALCULATORS ---
        SpecializedCalculatorDef(
            id = "age",
            title = "Age Calculator",
            subtitle = "Exact age in years, months, days, total weeks, and next birthday",
            category = SpecializedCategory.OTHER,
            keywords = listOf("age", "birthday", "how old", "date of birth", "years old", "calendar"),
            iconName = "DateRange"
        ),
        SpecializedCalculatorDef(
            id = "gpa",
            title = "GPA & Grade Calculator",
            subtitle = "Calculate cumulative Grade Point Average on 4.0 scale",
            category = SpecializedCategory.OTHER,
            keywords = listOf("gpa", "grade", "credits", "grade point average", "college", "school"),
            iconName = "School"
        )
    )

    fun findById(id: String): SpecializedCalculatorDef? {
        return allCalculators.find { it.id.equals(id, ignoreCase = true) }
    }

    fun search(query: String, category: SpecializedCategory = SpecializedCategory.ALL): List<SpecializedCalculatorDef> {
        val q = query.trim().lowercase()
        return allCalculators.filter { item ->
            val matchesCategory = category == SpecializedCategory.ALL || item.category == category
            val matchesQuery = q.isEmpty() ||
                    item.title.lowercase().contains(q) ||
                    item.subtitle.lowercase().contains(q) ||
                    item.keywords.any { it.contains(q) }
            matchesCategory && matchesQuery
        }
    }
}
