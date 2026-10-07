package com.allcalc.app.domain.converter

data class UnitDefinition(
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

fun linearUnit(name: String, symbol: String, factor: Double) =
    UnitDefinition(name, symbol, { v -> v * factor }, { v -> v / factor })

const val TIME_NOTE =
    "Month is treated as 30.437 days (1/12 of the 365.2425-day Gregorian year). " +
        "Year is the Gregorian average of 365.2425 days, so month and year conversions are approximate by definition."

const val DATA_NOTE =
    "Storage uses decimal prefixes (1 kB = 1,000 B, 1 MB = 1,000 kB, 1 GB = 1,000 MB). " +
        "IEC binary units are also provided (1 KiB = 1,024 B, 1 MiB = 1,024 KiB, 1 GiB = 1,024 MiB)."

enum class ConversionCategory(val title: String, val note: String?, val units: List<UnitDefinition>) {
    LENGTH(
        "Length", null,
        listOf(
            linearUnit("Millimeter", "mm", 0.001),
            linearUnit("Centimeter", "cm", 0.01),
            linearUnit("Meter", "m", 1.0),
            linearUnit("Kilometer", "km", 1000.0),
            linearUnit("Inch", "in", 0.0254),
            linearUnit("Foot", "ft", 0.3048),
            linearUnit("Yard", "yd", 0.9144),
            linearUnit("Mile", "mi", 1609.344),
            linearUnit("Nautical mile", "nmi", 1852.0)
        )
    ),
    WEIGHT(
        "Weight / Mass", null,
        listOf(
            linearUnit("Microgram", "µg", 1e-9),
            linearUnit("Milligram", "mg", 1e-6),
            linearUnit("Gram", "g", 0.001),
            linearUnit("Kilogram", "kg", 1.0),
            linearUnit("Tonne", "t", 1000.0),
            linearUnit("Ounce", "oz", 0.028349523125),
            linearUnit("Pound", "lb", 0.45359237),
            linearUnit("Stone", "st", 6.35029318)
        )
    ),
    AREA(
        "Area", null,
        listOf(
            linearUnit("Square millimeter", "mm²", 1e-6),
            linearUnit("Square centimeter", "cm²", 1e-4),
            linearUnit("Square meter", "m²", 1.0),
            linearUnit("Square kilometer", "km²", 1e6),
            linearUnit("Square inch", "in²", 0.00064516),
            linearUnit("Square foot", "ft²", 0.09290304),
            linearUnit("Square yard", "yd²", 0.83612736),
            linearUnit("Square mile", "mi²", 2589988.110336),
            linearUnit("Acre", "acre", 4046.8564224),
            linearUnit("Hectare", "ha", 10000.0)
        )
    ),
    VOLUME(
        "Volume", null,
        listOf(
            linearUnit("Milliliter", "ml", 1e-6),
            linearUnit("Liter", "l", 0.001),
            linearUnit("Cubic centimeter", "cm³", 1e-6),
            linearUnit("Cubic meter", "m³", 1.0),
            linearUnit("Cubic inch", "in³", 1.6387064e-5),
            linearUnit("Cubic foot", "ft³", 0.028316846592),
            linearUnit("Gallon (US)", "gal", 0.003785411784),
            linearUnit("Quart (US)", "qt", 0.000946352946),
            linearUnit("Pint (US)", "pt", 0.000473176473),
            linearUnit("Cup (US)", "cup", 0.0002365882365),
            linearUnit("Tablespoon", "tbsp", 1.478676478125e-5),
            linearUnit("Teaspoon", "tsp", 4.92892159375e-6)
        )
    ),
    TEMPERATURE(
        "Temperature", null,
        listOf(
            UnitDefinition("Celsius", "°C", { it }, { it }),
            UnitDefinition("Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
            UnitDefinition("Kelvin", "K", { it - 273.15 }, { it + 273.15 })
        )
    ),
    TIME(
        "Time", TIME_NOTE,
        listOf(
            linearUnit("Millisecond", "ms", 0.001),
            linearUnit("Second", "s", 1.0),
            linearUnit("Minute", "min", 60.0),
            linearUnit("Hour", "h", 3600.0),
            linearUnit("Day", "day", 86400.0),
            linearUnit("Week", "week", 604800.0),
            linearUnit("Month", "month", 2629746.0),
            linearUnit("Year", "year", 31556952.0)
        )
    ),
    SPEED(
        "Speed", null,
        listOf(
            linearUnit("Meters per second", "m/s", 1.0),
            linearUnit("Kilometers per hour", "km/h", 1.0 / 3.6),
            linearUnit("Miles per hour", "mph", 0.44704),
            linearUnit("Feet per second", "ft/s", 0.3048),
            linearUnit("Knot", "knot", 0.5144444444444445)
        )
    ),
    PRESSURE(
        "Pressure", null,
        listOf(
            linearUnit("Pascal", "Pa", 1.0),
            linearUnit("Kilopascal", "kPa", 1000.0),
            linearUnit("Bar", "bar", 100000.0),
            linearUnit("PSI", "psi", 6894.757293168),
            linearUnit("Atmosphere", "atm", 101325.0),
            linearUnit("Millimeter of mercury", "mmHg", 133.322387415)
        )
    ),
    ENERGY(
        "Energy", null,
        listOf(
            linearUnit("Joule", "J", 1.0),
            linearUnit("Kilojoule", "kJ", 1000.0),
            linearUnit("Calorie", "cal", 4.184),
            linearUnit("Kilocalorie", "kcal", 4184.0),
            linearUnit("Watt-hour", "Wh", 3600.0),
            linearUnit("Kilowatt-hour", "kWh", 3.6e6),
            linearUnit("Electronvolt", "eV", 1.602176634e-19)
        )
    ),
    POWER(
        "Power", null,
        listOf(
            linearUnit("Watt", "W", 1.0),
            linearUnit("Kilowatt", "kW", 1000.0),
            linearUnit("Megawatt", "MW", 1e6),
            linearUnit("Horsepower", "hp", 745.6998715822702)
        )
    ),
    DATA(
        "Data Storage", DATA_NOTE,
        listOf(
            linearUnit("Bit", "bit", 0.125),
            linearUnit("Byte", "B", 1.0),
            linearUnit("Kilobyte (decimal)", "kB", 1000.0),
            linearUnit("Megabyte (decimal)", "MB", 1e6),
            linearUnit("Gigabyte (decimal)", "GB", 1e9),
            linearUnit("Terabyte (decimal)", "TB", 1e12),
            linearUnit("Petabyte (decimal)", "PB", 1e15),
            linearUnit("Kibibyte (binary)", "KiB", 1024.0),
            linearUnit("Mebibyte (binary)", "MiB", 1048576.0),
            linearUnit("Gibibyte (binary)", "GiB", 1073741824.0),
            linearUnit("Tebibyte (binary)", "TiB", 1099511627776.0)
        )
    ),
    ANGLE(
        "Angle", null,
        listOf(
            linearUnit("Degree", "deg", 1.0),
            linearUnit("Radian", "rad", 180.0 / Math.PI),
            linearUnit("Gradian", "grad", 0.9)
        )
    )
}

object ConversionEngine {
    fun units(category: ConversionCategory): List<UnitDefinition> = category.units

    fun find(category: ConversionCategory, symbol: String): UnitDefinition? =
        category.units.firstOrNull { it.symbol == symbol }

    fun convert(value: Double, from: UnitDefinition, to: UnitDefinition): Double {
        if (from.symbol == to.symbol) return value
        val base = from.toBase(value)
        return to.fromBase(base)
    }

    fun convert(category: ConversionCategory, fromSymbol: String, toSymbol: String, value: Double): Double {
        val from = find(category, fromSymbol) ?: throw IllegalArgumentException("Unknown unit: $fromSymbol")
        val to = find(category, toSymbol) ?: throw IllegalArgumentException("Unknown unit: $toSymbol")
        return convert(value, from, to)
    }
}