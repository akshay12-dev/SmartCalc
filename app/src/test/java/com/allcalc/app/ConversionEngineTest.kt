package com.allcalc.app

import com.allcalc.app.domain.converter.ConversionCategory
import com.allcalc.app.domain.converter.ConversionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionEngineTest {

    @Test
    fun `converts kilograms to grams`() {
        assertEquals(1000.0, convert(ConversionCategory.WEIGHT, "kg", "g", 1.0), 1e-9)
    }

    @Test
    fun `converts feet to centimetres`() {
        assertEquals(30.48, convert(ConversionCategory.LENGTH, "ft", "cm", 1.0), 1e-9)
    }

    @Test
    fun `converts miles to kilometres`() {
        assertEquals(1.609344, convert(ConversionCategory.LENGTH, "mi", "km", 1.0), 1e-9)
    }

    @Test
    fun `converts acres to square metres exactly`() {
        assertEquals(4046.8564224, convert(ConversionCategory.AREA, "acre", "m²", 1.0), 1e-9)
    }

    @Test
    fun `converts gallons to litres`() {
        assertEquals(3.785411784, convert(ConversionCategory.VOLUME, "gal", "l", 1.0), 1e-9)
    }

    @Test
    fun `converts temperature with offsets`() {
        assertEquals(33.8, convert(ConversionCategory.TEMPERATURE, "°C", "°F", 1.0), 1e-9)
        assertEquals(273.15, convert(ConversionCategory.TEMPERATURE, "°C", "K", 0.0), 1e-9)
        assertEquals(0.0, convert(ConversionCategory.TEMPERATURE, "°F", "°C", 32.0), 1e-9)
    }

    @Test
    fun `same unit returns the input value even for temperatures`() {
        assertEquals(100.0, convert(ConversionCategory.TEMPERATURE, "°C", "°C", 100.0), 1e-9)
    }

    @Test
    fun `data uses decimal prefixes`() {
        assertEquals(1000.0, convert(ConversionCategory.DATA, "GB", "MB", 1.0), 1e-6)
        assertEquals(1e12, convert(ConversionCategory.DATA, "TB", "B", 1.0), 1e-3)
    }

    @Test
    fun `IEC binary units use powers of 1024`() {
        assertEquals(1024.0, convert(ConversionCategory.DATA, "GiB", "MiB", 1.0), 1e-6)
        assertEquals(1073741824.0, convert(ConversionCategory.DATA, "GiB", "B", 1.0), 1e-3)
    }

    @Test
    fun `month and year use Gregorian averages`() {
        assertEquals(30.437, convert(ConversionCategory.TIME, "month", "day", 1.0), 1e-3)
        assertEquals(365.2425, convert(ConversionCategory.TIME, "year", "day", 1.0), 1e-3)
    }

    @Test
    fun `converts speed`() {
        assertEquals(100.0, convert(ConversionCategory.SPEED, "km/h", "m/s", 360.0), 1e-6)
        assertEquals(12.96, convert(ConversionCategory.SPEED, "m/s", "km/h", 3.6), 1e-6)
    }

    private fun convert(
        category: ConversionCategory,
        from: String,
        to: String,
        value: Double
    ): Double = ConversionEngine.convert(category, from, to, value)
}