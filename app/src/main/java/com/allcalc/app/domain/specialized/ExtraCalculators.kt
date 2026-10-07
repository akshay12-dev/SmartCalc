package com.allcalc.app.domain.specialized

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.*

object ExtraCalculators {

    // -------------------------------------------------------------
    // FINANCIAL CALCULATORS
    // -------------------------------------------------------------

    // 1. Loan & Mortgage / EMI Calculator
    fun calculateLoan(principal: Double, annualRatePct: Double, termYears: Double): CalculationResult {
        if (principal <= 0.0 || termYears <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Principal and loan term must be greater than zero")
        }
        val monthlyRate = (annualRatePct / 100.0) / 12.0
        val totalMonths = (termYears * 12.0).roundToInt()

        val monthlyPayment: Double
        val totalPayment: Double
        val totalInterest: Double

        if (monthlyRate > 0.0) {
            val factor = (1.0 + monthlyRate).pow(totalMonths.toDouble())
            monthlyPayment = principal * (monthlyRate * factor) / (factor - 1.0)
            totalPayment = monthlyPayment * totalMonths
            totalInterest = totalPayment - principal
        } else {
            monthlyPayment = principal / totalMonths
            totalPayment = principal
            totalInterest = 0.0
        }

        val steps = listOf(
            "Principal: $principal, Annual Interest: $annualRatePct%, Term: $termYears years ($totalMonths months)",
            "Monthly Interest Rate r = $annualRatePct% / 12 = ${MathCalculators.formatNumber(monthlyRate * 100)}%",
            "EMI Formula: P · [r(1+r)ⁿ] / [(1+r)ⁿ - 1]",
            "Monthly Payment = ${MathCalculators.formatNumber(monthlyPayment)}"
        )

        val details = listOf(
            "Monthly Payment (EMI)" to MathCalculators.formatNumber(monthlyPayment),
            "Total Principal" to MathCalculators.formatNumber(principal),
            "Total Interest" to MathCalculators.formatNumber(totalInterest),
            "Total Payment" to MathCalculators.formatNumber(totalPayment),
            "Total Payments" to "$totalMonths months"
        )

        return CalculationResult(
            primaryResult = "Monthly: ${MathCalculators.formatNumber(monthlyPayment)}",
            details = details,
            steps = steps
        )
    }

    // 2. Simple & Compound Interest Calculator
    fun calculateInterest(
        principal: Double,
        annualRatePct: Double,
        years: Double,
        compoundingPerYear: Int // 1=annual, 4=quarterly, 12=monthly, 365=daily, 0=simple
    ): CalculationResult {
        if (principal <= 0.0 || years < 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Principal must be > 0 and time >= 0")
        }
        val r = annualRatePct / 100.0
        val totalAmount: Double
        val interestEarned: Double
        val steps = mutableListOf<String>()

        if (compoundingPerYear == 0) {
            // Simple interest: I = P * r * t
            interestEarned = principal * r * years
            totalAmount = principal + interestEarned
            steps.add("Simple Interest Formula: I = P · r · t")
            steps.add("Interest = $principal · $r · $years = $interestEarned")
            steps.add("Total Amount = $principal + $interestEarned = $totalAmount")
        } else {
            // Compound: A = P(1 + r/n)^(nt)
            val n = compoundingPerYear.toDouble()
            totalAmount = principal * (1.0 + r / n).pow(n * years)
            interestEarned = totalAmount - principal
            steps.add("Compound Interest Formula: A = P · (1 + r/n)^(n·t)")
            steps.add("Compounding periods per year n = $compoundingPerYear")
            steps.add("Total Amount = $principal · (1 + $r / $n)^($n · $years) = $totalAmount")
            steps.add("Interest Earned = A - P = $interestEarned")
        }

        return CalculationResult(
            primaryResult = "Total: ${MathCalculators.formatNumber(totalAmount)}",
            details = listOf(
                "Total Future Value" to MathCalculators.formatNumber(totalAmount),
                "Interest Earned" to MathCalculators.formatNumber(interestEarned),
                "Principal" to MathCalculators.formatNumber(principal)
            ),
            steps = steps
        )
    }

    // 3. Tip & Bill Split Calculator
    fun calculateTip(bill: Double, tipPct: Double, splitCount: Int): CalculationResult {
        if (bill <= 0.0) return CalculationResult("Error", isSuccess = false, errorMessage = "Bill amount must be positive")
        val people = splitCount.coerceAtLeast(1)
        val tipAmount = bill * (tipPct / 100.0)
        val total = bill + tipAmount
        val perPerson = total / people
        val tipPerPerson = tipAmount / people

        val steps = listOf(
            "Bill Amount: $bill, Tip: $tipPct%, Split: $people people",
            "Tip Amount = $bill · ($tipPct / 100) = $tipAmount",
            "Total Amount = $bill + $tipAmount = $total",
            "Per Person = $total / $people = $perPerson"
        )

        return CalculationResult(
            primaryResult = "${MathCalculators.formatNumber(perPerson)} / person",
            details = listOf(
                "Total Bill" to MathCalculators.formatNumber(total),
                "Total Tip" to MathCalculators.formatNumber(tipAmount),
                "Per Person Total" to MathCalculators.formatNumber(perPerson),
                "Per Person Tip" to MathCalculators.formatNumber(tipPerPerson)
            ),
            steps = steps
        )
    }

    // 4. Investment / ROI Calculator
    fun calculateInvestment(startingAmount: Double, monthlyContribution: Double, ratePct: Double, years: Double): CalculationResult {
        if (startingAmount < 0.0 || years <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Starting amount >= 0 and years > 0 required")
        }
        val r = (ratePct / 100.0) / 12.0
        val totalMonths = (years * 12.0).roundToInt()

        var futureValue = startingAmount * (1.0 + r).pow(totalMonths.toDouble())
        if (r > 0.0) {
            futureValue += monthlyContribution * ((1.0 + r).pow(totalMonths.toDouble()) - 1.0) / r
        } else {
            futureValue += monthlyContribution * totalMonths
        }

        val totalDeposited = startingAmount + (monthlyContribution * totalMonths)
        val totalReturns = futureValue - totalDeposited

        return CalculationResult(
            primaryResult = "Value: ${MathCalculators.formatNumber(futureValue)}",
            details = listOf(
                "Future Investment Value" to MathCalculators.formatNumber(futureValue),
                "Total Principal Invested" to MathCalculators.formatNumber(totalDeposited),
                "Total Interest/Growth" to MathCalculators.formatNumber(totalReturns),
                "ROI (Return on Investment)" to if (totalDeposited > 0) "${MathCalculators.formatNumber((totalReturns / totalDeposited) * 100.0)}%" else "0%"
            ),
            steps = listOf(
                "Starting: $startingAmount, Monthly: $monthlyContribution, Return: $ratePct% over $years years",
                "Compound interest applied on monthly intervals",
                "Total Future Value: ${MathCalculators.formatNumber(futureValue)}"
            )
        )
    }

    // -------------------------------------------------------------
    // FITNESS & HEALTH CALCULATORS
    // -------------------------------------------------------------

    // 5. BMI Calculator (Body Mass Index)
    fun calculateBmi(weightKg: Double, heightCm: Double): CalculationResult {
        if (weightKg <= 0.0 || heightCm <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Weight and height must be positive")
        }
        val heightM = heightCm / 100.0
        val bmi = weightKg / (heightM * heightM)

        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal weight"
            bmi < 30.0 -> "Overweight"
            bmi < 35.0 -> "Obese (Class 1)"
            bmi < 40.0 -> "Obese (Class 2)"
            else -> "Severely Obese (Class 3)"
        }

        val healthyMin = 18.5 * heightM * heightM
        val healthyMax = 24.9 * heightM * heightM

        val steps = listOf(
            "Formula: BMI = Weight (kg) / [Height (m)]²",
            "Height: $heightCm cm = $heightM m",
            "BMI = $weightKg / ($heightM)² = $weightKg / ${heightM * heightM} = ${MathCalculators.formatNumber(bmi)}",
            "Classification: $category"
        )

        return CalculationResult(
            primaryResult = "BMI ${MathCalculators.formatNumber(bmi)} ($category)",
            details = listOf(
                "BMI Score" to MathCalculators.formatNumber(bmi),
                "Category" to category,
                "Healthy Weight Range" to "${MathCalculators.formatNumber(healthyMin)} kg - ${MathCalculators.formatNumber(healthyMax)} kg",
                "Height" to "$heightCm cm",
                "Weight" to "$weightKg kg"
            ),
            steps = steps
        )
    }

    // 6. Calorie & BMR Calculator (Mifflin-St Jeor)
    fun calculateBmr(
        gender: String, // "male", "female"
        weightKg: Double,
        heightCm: Double,
        age: Int,
        activityLevel: String // "sedentary", "light", "moderate", "active", "very_active"
    ): CalculationResult {
        if (weightKg <= 0.0 || heightCm <= 0.0 || age <= 0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Parameters must be positive")
        }

        // Mifflin-St Jeor Equation
        // BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age(y) + s (+5 for male, -161 for female)
        val s = if (gender.equals("male", ignoreCase = true)) 5.0 else -161.0
        val bmr = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age + s

        val multiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active" -> 1.9
            else -> 1.2
        }

        val tdee = bmr * multiplier
        val weightLoss = tdee - 500.0
        val weightGain = tdee + 500.0

        val steps = listOf(
            "Mifflin-St Jeor Formula: BMR = 10·weight + 6.25·height - 5·age + $s",
            "Basal Metabolic Rate (BMR) = ${MathCalculators.formatNumber(bmr)} kcal/day",
            "Daily Energy Expenditure (TDEE) = BMR · $multiplier = ${MathCalculators.formatNumber(tdee)} kcal/day"
        )

        return CalculationResult(
            primaryResult = "${MathCalculators.formatNumber(tdee)} kcal / day",
            details = listOf(
                "Maintenance Calories (TDEE)" to "${MathCalculators.formatNumber(tdee)} kcal",
                "Basal Metabolic Rate (BMR)" to "${MathCalculators.formatNumber(bmr)} kcal",
                "Weight Loss (-0.5 kg/week)" to "${MathCalculators.formatNumber(weightLoss)} kcal",
                "Weight Gain (+0.5 kg/week)" to "${MathCalculators.formatNumber(weightGain)} kcal"
            ),
            steps = steps
        )
    }

    // 7. Body Fat Calculator (US Navy method)
    fun calculateBodyFat(
        gender: String,
        heightCm: Double,
        waistCm: Double,
        neckCm: Double,
        hipCm: Double? = null // required for female
    ): CalculationResult {
        if (heightCm <= 0.0 || waistCm <= 0.0 || neckCm <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Measurements must be positive")
        }

        val isMale = gender.equals("male", ignoreCase = true)
        val bodyFatPct: Double

        if (isMale) {
            // Male formula: 495 / (1.0324 - 0.19077*log10(waist - neck) + 0.15456*log10(height)) - 450
            val diff = waistCm - neckCm
            if (diff <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Waist must exceed neck circumference")
            bodyFatPct = 495.0 / (1.0324 - 0.19077 * log10(diff) + 0.15456 * log10(heightCm)) - 450.0
        } else {
            val hip = hipCm ?: (waistCm * 1.1)
            val sumDiff = waistCm + hip - neckCm
            if (sumDiff <= 0) return CalculationResult("Error", isSuccess = false, errorMessage = "Invalid circumference measurements")
            bodyFatPct = 495.0 / (1.29579 - 0.35004 * log10(sumDiff) + 0.22100 * log10(heightCm)) - 450.0
        }

        val clamped = bodyFatPct.coerceIn(2.0, 70.0)
        val category = if (isMale) {
            when {
                clamped < 6 -> "Essential fat"
                clamped < 14 -> "Athletic"
                clamped < 18 -> "Fitness"
                clamped < 25 -> "Average"
                else -> "Obese"
            }
        } else {
            when {
                clamped < 14 -> "Essential fat"
                clamped < 21 -> "Athletic"
                clamped < 25 -> "Fitness"
                clamped < 32 -> "Average"
                else -> "Obese"
            }
        }

        return CalculationResult(
            primaryResult = "${MathCalculators.formatNumber(clamped)}% ($category)",
            details = listOf(
                "Body Fat Percentage" to "${MathCalculators.formatNumber(clamped)}%",
                "Category" to category,
                "Method" to "U.S. Navy Circumference Formula"
            ),
            steps = listOf(
                "U.S. Navy Body Fat Method",
                "Gender: $gender, Height: $heightCm cm, Waist: $waistCm cm, Neck: $neckCm cm",
                "Result: ${MathCalculators.formatNumber(clamped)}%"
            )
        )
    }

    // -------------------------------------------------------------
    // OTHER CALCULATORS (Age, Date Difference, GPA)
    // -------------------------------------------------------------

    // 8. Age Calculator
    fun calculateAge(birthYear: Int, birthMonth: Int, birthDay: Int): CalculationResult {
        return try {
            val birthDate = LocalDate.of(birthYear, birthMonth, birthDay)
            val today = LocalDate.now()
            if (birthDate.isAfter(today)) {
                return CalculationResult("Error", isSuccess = false, errorMessage = "Birthdate cannot be in the future")
            }

            val period = Period.between(birthDate, today)
            val totalDays = ChronoUnit.DAYS.between(birthDate, today)
            val totalWeeks = totalDays / 7
            val totalMonths = period.years * 12 + period.months

            val nextBirthday = birthDate.withYear(today.year).let {
                if (it.isBefore(today) || it.isEqual(today)) it.plusYears(1) else it
            }
            val daysToNextBirthday = ChronoUnit.DAYS.between(today, nextBirthday)

            val primary = "${period.years} years, ${period.months} months, ${period.days} days"

            CalculationResult(
                primaryResult = primary,
                details = listOf(
                    "Age" to primary,
                    "Total Months" to "$totalMonths months",
                    "Total Weeks" to "$totalWeeks weeks",
                    "Total Days" to "$totalDays days",
                    "Next Birthday In" to "$daysToNextBirthday days"
                ),
                steps = listOf(
                    "Birth Date: $birthDate",
                    "Current Date: $today",
                    "Exact difference: ${period.years} years, ${period.months} months, ${period.days} days"
                )
            )
        } catch (e: Exception) {
            CalculationResult("Error", isSuccess = false, errorMessage = "Invalid date: ${e.message}")
        }
    }

    // 9. GPA / Grade Calculator
    fun calculateGpa(gradesWithCredits: List<Pair<Double, Double>>): CalculationResult {
        if (gradesWithCredits.isEmpty()) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Add at least one course grade and credit")
        }
        var totalPoints = 0.0
        var totalCredits = 0.0

        for ((gradePoint, credits) in gradesWithCredits) {
            totalPoints += gradePoint * credits
            totalCredits += credits
        }

        if (totalCredits <= 0.0) {
            return CalculationResult("Error", isSuccess = false, errorMessage = "Total credits must be greater than zero")
        }

        val gpa = totalPoints / totalCredits
        val steps = listOf(
            "Total Grade Points = $totalPoints",
            "Total Credits = $totalCredits",
            "GPA = Total Points / Total Credits = $totalPoints / $totalCredits = ${MathCalculators.formatNumber(gpa)}"
        )

        return CalculationResult(
            primaryResult = "GPA: ${MathCalculators.formatNumber(gpa)}",
            details = listOf(
                "Cumulative GPA" to MathCalculators.formatNumber(gpa),
                "Total Credits" to MathCalculators.formatNumber(totalCredits),
                "Total Quality Points" to MathCalculators.formatNumber(totalPoints)
            ),
            steps = steps
        )
    }
}
