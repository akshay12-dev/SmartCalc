package com.allcalc.app.ui.specialized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allcalc.app.data.local.HistoryEntity
import com.allcalc.app.data.local.HistoryTypes
import com.allcalc.app.domain.specialized.*
import com.allcalc.app.ui.common.ScreenHeader
import com.allcalc.app.ui.history.HistoryViewModel
import com.allcalc.app.utils.AppViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SpecializedRunnerScreen(
    calcId: String,
    onBack: () -> Unit
) {
    val calcDef = remember(calcId) { SpecializedCatalog.findById(calcId) }
    if (calcDef == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Calculator Not Found", onHome = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Calculator '$calcId' was not found.")
            }
        }
        return
    }

    val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider)
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var calculationResult by remember { mutableStateOf<CalculationResult?>(null) }
    var expressionSummary by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val onPerformCalc: (String, CalculationResult) -> Unit = { expr, res ->
        expressionSummary = expr
        calculationResult = res
        copied = false
        saved = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = calcDef.title,
            onHome = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subtitle banner
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = calcDef.getIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = calcDef.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Interactive Input Form tailored to calcId
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpecializedFormDispatcher(
                        calcId = calcId,
                        onCalculate = onPerformCalc
                    )
                }
            }

            // Calculation Results
            val res = calculationResult
            if (res != null) {
                if (!res.isSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Calculation Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = res.errorMessage ?: "Invalid input values",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    // Success Result Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Result",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = res.primaryResult,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons (Copy, Save)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(res.primaryResult))
                                        copied = true
                                        coroutineScope.launch {
                                            delay(1800)
                                            copied = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (copied) "Copied!" else "Copy")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        if (!saved) {
                                            coroutineScope.launch {
                                                historyViewModel.add(
                                                    HistoryEntity(
                                                        type = HistoryTypes.CALC,
                                                        category = calcDef.category.name,
                                                        label = calcDef.title,
                                                        expression = expressionSummary,
                                                        result = res.primaryResult
                                                    )
                                                )
                                                saved = true
                                                delay(1800)
                                                saved = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (saved) Icons.Filled.Check else Icons.Filled.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (saved) "Saved!" else "Save History")
                                }
                            }
                        }
                    }

                    // Key Details Grid
                    if (res.details.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Detailed Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                res.details.forEach { (label, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // Step-by-Step Derivation
                    if (res.steps.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Step-by-Step Solution",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                res.steps.forEachIndexed { index, step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = step,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecializedFormDispatcher(
    calcId: String,
    onCalculate: (String, CalculationResult) -> Unit
) {
    when (calcId) {
        "fraction" -> FractionForm(onCalculate)
        "percentage" -> PercentageForm(onCalculate)
        "percent_error" -> PercentErrorForm(onCalculate)
        "quadratic" -> QuadraticForm(onCalculate)
        "exponent" -> ExponentForm(onCalculate)
        "root" -> RootForm(onCalculate)
        "log" -> LogForm(onCalculate)
        "ratio" -> RatioForm(onCalculate)
        "lcm_gcf" -> LcmGcfForm(onCalculate)
        "factor" -> FactorForm(onCalculate)
        "rounding" -> RoundingForm(onCalculate)
        "matrix" -> MatrixForm(onCalculate)
        "scientific_notation" -> ScientificNotationForm(onCalculate)
        "big_number" -> BigNumberForm(onCalculate)
        "rng" -> RngForm(onCalculate)
        "binary_hex" -> BinaryHexForm(onCalculate)
        "half_life" -> HalfLifeForm(onCalculate)

        "descriptive_stats", "mean_median_mode" -> DatasetStatsForm(calcId, onCalculate)
        "std_dev" -> StdDevForm(onCalculate)
        "permutation_combination" -> PermutationCombinationForm(onCalculate)
        "probability" -> ProbabilityForm(onCalculate)
        "z_score" -> ZScoreForm(onCalculate)
        "confidence_interval" -> ConfidenceIntervalForm(onCalculate)
        "sequence" -> SequenceForm(onCalculate)
        "sample_size" -> SampleSizeForm(onCalculate)

        "pythagorean" -> PythagoreanForm(onCalculate)
        "right_triangle" -> RightTriangleForm(onCalculate)
        "triangle" -> TriangleForm(onCalculate)
        "circle" -> CircleForm(onCalculate)
        "area_2d" -> Area2DForm(onCalculate)
        "volume" -> VolumeForm(isSurfaceArea = false, onCalculate)
        "surface_area" -> VolumeForm(isSurfaceArea = true, onCalculate)
        "slope" -> SlopeForm(onCalculate)
        "distance" -> DistanceForm(onCalculate)

        // Financial
        "loan" -> LoanForm(onCalculate)
        "interest" -> InterestForm(onCalculate)
        "tip" -> TipForm(onCalculate)
        "investment" -> InvestmentForm(onCalculate)

        // Fitness & Health
        "bmi" -> BmiForm(onCalculate)
        "calorie_bmr" -> CalorieBmrForm(onCalculate)
        "body_fat" -> BodyFatForm(onCalculate)

        // Other
        "age" -> AgeForm(onCalculate)
        "gpa" -> GpaForm(onCalculate)

        else -> Text("Calculator form in development")
    }
}

// -------------------------------------------------------------
// MATH FORMS
// -------------------------------------------------------------

@Composable
private fun FractionForm(onCalculate: (String, CalculationResult) -> Unit) {
    var n1 by remember { mutableStateOf("1") }
    var d1 by remember { mutableStateOf("2") }
    var n2 by remember { mutableStateOf("1") }
    var d2 by remember { mutableStateOf("4") }
    var op by remember { mutableStateOf("+") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = n1,
                    onValueChange = { n1 = it },
                    label = { Text("Num 1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = d1,
                    onValueChange = { d1 = it },
                    label = { Text("Den 1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("+", "-", "×", "÷").forEach { sign ->
                    val actualOp = if (sign == "×") "*" else if (sign == "÷") "/" else sign
                    FilledIconToggleButton(
                        checked = op == actualOp,
                        onCheckedChange = { op = actualOp },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(sign, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = n2,
                    onValueChange = { n2 = it },
                    label = { Text("Num 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = d2,
                    onValueChange = { d2 = it },
                    label = { Text("Den 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = {
                val num1 = n1.toLongOrNull() ?: 0L
                val den1 = d1.toLongOrNull() ?: 1L
                val num2 = n2.toLongOrNull() ?: 0L
                val den2 = d2.toLongOrNull() ?: 1L
                val expr = "($num1/$den1) $op ($num2/$den2)"
                val res = MathCalculators.calculateFraction(num1, den1, num2, den2, op)
                onCalculate(expr, res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Fraction")
        }
    }
}

@Composable
private fun PercentageForm(onCalculate: (String, CalculationResult) -> Unit) {
    var mode by remember { mutableStateOf(1) }
    var x by remember { mutableStateOf("15") }
    var y by remember { mutableStateOf("200") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabRow(selectedTabIndex = mode - 1) {
            Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text("X% of Y") })
            Tab(selected = mode == 2, onClick = { mode = 2 }, text = { Text("X is ?% of Y") })
            Tab(selected = mode == 3, onClick = { mode = 3 }, text = { Text("% Change") })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = x,
                onValueChange = { x = it },
                label = { Text(if (mode == 1) "Percentage X (%)" else "Value X") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = y,
                onValueChange = { y = it },
                label = { Text(if (mode == 3) "New Value Y" else "Total / Base Y") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Button(
            onClick = {
                val valX = x.toDoubleOrNull() ?: 0.0
                val valY = y.toDoubleOrNull() ?: 0.0
                val expr = when (mode) {
                    1 -> "$valX% of $valY"
                    2 -> "$valX is what % of $valY"
                    else -> "Change from $valX to $valY"
                }
                val res = MathCalculators.calculatePercentage(mode, valX, valY)
                onCalculate(expr, res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Percentage")
        }
    }
}

@Composable
private fun PercentErrorForm(onCalculate: (String, CalculationResult) -> Unit) {
    var exp by remember { mutableStateOf("9.2") }
    var theo by remember { mutableStateOf("9.8") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = exp,
            onValueChange = { exp = it },
            label = { Text("Experimental / Measured Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = theo,
            onValueChange = { theo = it },
            label = { Text("Theoretical / True Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                val e = exp.toDoubleOrNull() ?: 0.0
                val t = theo.toDoubleOrNull() ?: 0.0
                val res = MathCalculators.calculatePercentError(e, t)
                onCalculate("Percent error of $e vs $t", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Percent Error")
        }
    }
}

@Composable
private fun QuadraticForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a by remember { mutableStateOf("1") }
    var b by remember { mutableStateOf("-5") }
    var c by remember { mutableStateOf("6") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Standard Form: ax² + bx + c = 0", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("a") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = b,
                onValueChange = { b = it },
                label = { Text("b") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = c,
                onValueChange = { c = it },
                label = { Text("c") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valA = a.toDoubleOrNull() ?: 1.0
                val valB = b.toDoubleOrNull() ?: 0.0
                val valC = c.toDoubleOrNull() ?: 0.0
                val res = MathCalculators.calculateQuadratic(valA, valB, valC)
                onCalculate("${valA}x² + ${valB}x + $valC = 0", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Quadratic Equation")
        }
    }
}

@Composable
private fun ExponentForm(onCalculate: (String, CalculationResult) -> Unit) {
    var base by remember { mutableStateOf("2") }
    var exp by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = base,
                onValueChange = { base = it },
                label = { Text("Base (x)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = exp,
                onValueChange = { exp = it },
                label = { Text("Exponent (y)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val b = base.toDoubleOrNull() ?: 0.0
                val e = exp.toDoubleOrNull() ?: 0.0
                val res = MathCalculators.calculateExponent(b, e)
                onCalculate("$b ^ $e", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Power")
        }
    }
}

@Composable
private fun RootForm(onCalculate: (String, CalculationResult) -> Unit) {
    var x by remember { mutableStateOf("64") }
    var n by remember { mutableStateOf("3") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = n,
                onValueChange = { n = it },
                label = { Text("Degree (n)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = x,
                onValueChange = { x = it },
                label = { Text("Radicand (x)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(2f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val rad = x.toDoubleOrNull() ?: 0.0
                val deg = n.toDoubleOrNull() ?: 2.0
                val res = MathCalculators.calculateRoot(rad, deg)
                onCalculate("Root $deg of $rad", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Root")
        }
    }
}

@Composable
private fun LogForm(onCalculate: (String, CalculationResult) -> Unit) {
    var x by remember { mutableStateOf("100") }
    var base by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = x,
                onValueChange = { x = it },
                label = { Text("Argument (x)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
            OutlinedTextField(
                value = base,
                onValueChange = { base = it },
                label = { Text("Base (b)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { base = "10" }, label = { Text("Base 10") })
            AssistChip(onClick = { base = "2.718282" }, label = { Text("ln (e)") })
            AssistChip(onClick = { base = "2" }, label = { Text("Base 2") })
        }
        Button(
            onClick = {
                val valX = x.toDoubleOrNull() ?: 1.0
                val valB = base.toDoubleOrNull() ?: 10.0
                val res = MathCalculators.calculateLog(valX, valB)
                onCalculate("log_$valB($valX)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Logarithm")
        }
    }
}

@Composable
private fun RatioForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a by remember { mutableStateOf("4") }
    var b by remember { mutableStateOf("12") }
    var c by remember { mutableStateOf("7") }
    var d by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Proportion A : B = C : D (Leave any one empty to solve)", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("A") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = b,
                onValueChange = { b = it },
                label = { Text("B") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = c,
                onValueChange = { c = it },
                label = { Text("C") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = d,
                onValueChange = { d = it },
                label = { Text("D") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valA = a.toDoubleOrNull()
                val valB = b.toDoubleOrNull()
                val valC = c.toDoubleOrNull()
                val valD = d.toDoubleOrNull()
                val res = MathCalculators.calculateRatio(valA, valB, valC, valD)
                onCalculate("Ratio $a : $b = $c : $d", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Ratio")
        }
    }
}

@Composable
private fun LcmGcfForm(onCalculate: (String, CalculationResult) -> Unit) {
    var input by remember { mutableStateOf("24, 36, 60") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Integers (comma or space separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                val list = input.split(",", " ", "\n", ";")
                    .mapNotNull { it.trim().toLongOrNull() }
                val res = MathCalculators.calculateLcmGcf(list)
                onCalculate("LCM & GCF of $input", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Find LCM & GCF")
        }
    }
}

@Composable
private fun FactorForm(onCalculate: (String, CalculationResult) -> Unit) {
    var n by remember { mutableStateOf("120") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = n,
            onValueChange = { n = it },
            label = { Text("Integer (n)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                val valN = n.toLongOrNull() ?: 1L
                val res = MathCalculators.calculateFactors(valN)
                onCalculate("Factors of $valN", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Find Factors & Primes")
        }
    }
}

@Composable
private fun RoundingForm(onCalculate: (String, CalculationResult) -> Unit) {
    var value by remember { mutableStateOf("3.14159265") }
    var places by remember { mutableStateOf("3") }
    var mode by remember { mutableStateOf("Half Up") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Number to Round") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = places,
                onValueChange = { places = it },
                label = { Text("Decimal Places") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Half Up", "Up (Ceil)", "Down (Floor)", "Half Even (Banker's)").forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m) }
                )
            }
        }
        Button(
            onClick = {
                val v = value.toDoubleOrNull() ?: 0.0
                val p = places.toIntOrNull() ?: 2
                val res = MathCalculators.calculateRounding(v, p, mode)
                onCalculate("Round $value to $p places ($mode)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Round Number")
        }
    }
}

@Composable
private fun MatrixForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a11 by remember { mutableStateOf("1") }
    var a12 by remember { mutableStateOf("2") }
    var a21 by remember { mutableStateOf("3") }
    var a22 by remember { mutableStateOf("4") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("2×2 Matrix Elements", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a11,
                onValueChange = { a11 = it },
                label = { Text("Row 1, Col 1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = a12,
                onValueChange = { a12 = it },
                label = { Text("Row 1, Col 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a21,
                onValueChange = { a21 = it },
                label = { Text("Row 2, Col 1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = a22,
                onValueChange = { a22 = it },
                label = { Text("Row 2, Col 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val v11 = a11.toDoubleOrNull() ?: 0.0
                val v12 = a12.toDoubleOrNull() ?: 0.0
                val v21 = a21.toDoubleOrNull() ?: 0.0
                val v22 = a22.toDoubleOrNull() ?: 0.0
                val res = MathCalculators.calculateMatrix2x2(v11, v12, v21, v22)
                onCalculate("Matrix [$v11 $v12 ; $v21 $v22]", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Determinant & Inverse")
        }
    }
}

@Composable
private fun ScientificNotationForm(onCalculate: (String, CalculationResult) -> Unit) {
    var value by remember { mutableStateOf("299792458") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Decimal Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { value = "299792458" }, label = { Text("Speed of Light") })
            AssistChip(onClick = { value = "0.0000000000667" }, label = { Text("Gravity G") })
            AssistChip(onClick = { value = "602214076000000000000000" }, label = { Text("Avogadro") })
        }
        Button(
            onClick = {
                val v = value.toDoubleOrNull() ?: 0.0
                val res = MathCalculators.calculateScientificNotation(v)
                onCalculate("Scientific notation of $value", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Convert to Scientific Notation")
        }
    }
}

@Composable
private fun BigNumberForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a by remember { mutableStateOf("12345678901234567890") }
    var b by remember { mutableStateOf("98765432109876543210") }
    var op by remember { mutableStateOf("+") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = a,
            onValueChange = { a = it },
            label = { Text("Big Integer A") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("+", "-", "×", "÷", "%").forEach { s ->
                val actual = if (s == "×") "*" else if (s == "÷") "/" else s
                FilledIconToggleButton(
                    checked = op == actual,
                    onCheckedChange = { op = actual },
                    modifier = Modifier.size(38.dp)
                ) {
                    Text(s, fontWeight = FontWeight.Bold)
                }
            }
        }
        OutlinedTextField(
            value = b,
            onValueChange = { b = it },
            label = { Text("Big Integer B") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val res = MathCalculators.calculateBigNumber(a, b, op)
                onCalculate("BigInt: $a $op $b", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Arbitrary Precision")
        }
    }
}

@Composable
private fun RngForm(onCalculate: (String, CalculationResult) -> Unit) {
    var min by remember { mutableStateOf("1") }
    var max by remember { mutableStateOf("100") }
    var count by remember { mutableStateOf("1") }
    var unique by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = min,
                onValueChange = { min = it },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = max,
                onValueChange = { max = it },
                label = { Text("Max") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = count,
                onValueChange = { count = it },
                label = { Text("Count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = unique, onCheckedChange = { unique = it })
            Text("Unique values (no duplicates)")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { min = "1"; max = "6"; count = "1" }, label = { Text("Roll D6") })
            AssistChip(onClick = { min = "1"; max = "20"; count = "1" }, label = { Text("Roll D20") })
            AssistChip(onClick = { min = "1"; max = "100"; count = "5"; unique = true }, label = { Text("Lottery 5/100") })
        }

        Button(
            onClick = {
                val minVal = min.toLongOrNull() ?: 1L
                val maxVal = max.toLongOrNull() ?: 100L
                val cnt = (count.toIntOrNull() ?: 1).coerceIn(1, 100)
                val res = MathCalculators.generateRandom(minVal, maxVal, cnt, unique)
                onCalculate("RNG [$minVal, $maxVal] x$cnt", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Random Numbers")
        }
    }
}

@Composable
private fun BinaryHexForm(onCalculate: (String, CalculationResult) -> Unit) {
    var input by remember { mutableStateOf("255") }
    var base by remember { mutableStateOf(10) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Number in Base $base") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2 to "Binary (2)", 8 to "Octal (8)", 10 to "Decimal (10)", 16 to "Hex (16)").forEach { (b, label) ->
                FilterChip(
                    selected = base == b,
                    onClick = { base = b },
                    label = { Text(label) }
                )
            }
        }
        Button(
            onClick = {
                val res = MathCalculators.calculateBases(input, base)
                onCalculate("Base $base conversion of $input", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Convert Bases")
        }
    }
}

@Composable
private fun HalfLifeForm(onCalculate: (String, CalculationResult) -> Unit) {
    var n0 by remember { mutableStateOf("100") }
    var nt by remember { mutableStateOf("") }
    var hl by remember { mutableStateOf("5.73") }
    var time by remember { mutableStateOf("11.46") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Formula: N(t) = N₀ · (1/2)^(t / t½) (Leave any one empty to solve)", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = n0,
                onValueChange = { n0 = it },
                label = { Text("Initial (N₀)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = nt,
                onValueChange = { nt = it },
                label = { Text("Remaining N(t)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hl,
                onValueChange = { hl = it },
                label = { Text("Half-Life (t½)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Elapsed Time (t)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valN0 = n0.toDoubleOrNull()
                val valNt = nt.toDoubleOrNull()
                val valHl = hl.toDoubleOrNull()
                val valT = time.toDoubleOrNull()
                val res = MathCalculators.calculateHalfLife(valN0, valNt, valHl, valT)
                onCalculate("Half-Life calculation", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Half-Life")
        }
    }
}

// -------------------------------------------------------------
// STATISTICS FORMS
// -------------------------------------------------------------

@Composable
private fun DatasetStatsForm(calcId: String, onCalculate: (String, CalculationResult) -> Unit) {
    var dataset by remember { mutableStateOf("12, 15, 18, 20, 22, 25, 28, 30") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = dataset,
            onValueChange = { dataset = it },
            label = { Text("Dataset (comma, space or newline separated)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { dataset = "72, 85, 90, 68, 95, 88, 79, 92" }, label = { Text("Exam Scores") })
            AssistChip(onClick = { dataset = "10, 12, 12, 14, 15, 18, 21, 24" }, label = { Text("Sample A") })
        }
        Button(
            onClick = {
                val list = StatisticsCalculators.parseData(dataset)
                val res = StatisticsCalculators.calculateDescriptiveStats(list)
                onCalculate("Dataset stats (n=${list.size})", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (calcId == "mean_median_mode") "Calculate Mean, Median & Mode" else "Analyze Dataset Statistics")
        }
    }
}

@Composable
private fun StdDevForm(onCalculate: (String, CalculationResult) -> Unit) {
    var dataset by remember { mutableStateOf("10, 20, 30, 40, 50") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = dataset,
            onValueChange = { dataset = it },
            label = { Text("Values (comma or space separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val list = StatisticsCalculators.parseData(dataset)
                val res = StatisticsCalculators.calculateDescriptiveStats(list)
                onCalculate("Standard deviation of dataset", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Standard Deviation (s & σ)")
        }
    }
}

@Composable
private fun PermutationCombinationForm(onCalculate: (String, CalculationResult) -> Unit) {
    var n by remember { mutableStateOf("10") }
    var r by remember { mutableStateOf("3") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = n,
                onValueChange = { n = it },
                label = { Text("Total items (n)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = r,
                onValueChange = { r = it },
                label = { Text("Chosen items (r)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valN = n.toLongOrNull() ?: 1L
                val valR = r.toLongOrNull() ?: 1L
                val res = StatisticsCalculators.calculatePermutationCombination(valN, valR)
                onCalculate("nPr & nCr ($valN, $valR)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Permutation & Combination")
        }
    }
}

@Composable
private fun ProbabilityForm(onCalculate: (String, CalculationResult) -> Unit) {
    var fav by remember { mutableStateOf("3") }
    var total by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fav,
                onValueChange = { fav = it },
                label = { Text("Favorable Outcomes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = total,
                onValueChange = { total = it },
                label = { Text("Total Outcomes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { fav = "1"; total = "6" }, label = { Text("Roll a 6 (1/6)") })
            AssistChip(onClick = { fav = "1"; total = "2" }, label = { Text("Coin Flip (1/2)") })
            AssistChip(onClick = { fav = "4"; total = "52" }, label = { Text("Draw Ace (4/52)") })
        }
        Button(
            onClick = {
                val f = fav.toDoubleOrNull() ?: 0.0
                val t = total.toDoubleOrNull() ?: 1.0
                val res = StatisticsCalculators.calculateProbability(f, t)
                onCalculate("Probability $f / $t", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Probability & Odds")
        }
    }
}

@Composable
private fun ZScoreForm(onCalculate: (String, CalculationResult) -> Unit) {
    var x by remember { mutableStateOf("85") }
    var mean by remember { mutableStateOf("70") }
    var sd by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = x,
                onValueChange = { x = it },
                label = { Text("Raw Value (X)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = mean,
                onValueChange = { mean = it },
                label = { Text("Mean (μ)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = sd,
                onValueChange = { sd = it },
                label = { Text("Std Dev (σ)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valX = x.toDoubleOrNull() ?: 0.0
                val valMean = mean.toDoubleOrNull() ?: 0.0
                val valSd = sd.toDoubleOrNull() ?: 1.0
                val res = StatisticsCalculators.calculateZScore(valX, valMean, valSd)
                onCalculate("Z-Score for X=$valX, μ=$valMean, σ=$valSd", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Z-Score & Percentile")
        }
    }
}

@Composable
private fun ConfidenceIntervalForm(onCalculate: (String, CalculationResult) -> Unit) {
    var mean by remember { mutableStateOf("50") }
    var sd by remember { mutableStateOf("5") }
    var n by remember { mutableStateOf("100") }
    var confLevel by remember { mutableStateOf(95.0) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = mean,
                onValueChange = { mean = it },
                label = { Text("Mean (x̄)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = sd,
                onValueChange = { sd = it },
                label = { Text("Std Dev (s)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = n,
                onValueChange = { n = it },
                label = { Text("Sample Size (n)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(90.0 to "90%", 95.0 to "95%", 99.0 to "99%").forEach { (lvl, title) ->
                FilterChip(
                    selected = confLevel == lvl,
                    onClick = { confLevel = lvl },
                    label = { Text(title) }
                )
            }
        }
        Button(
            onClick = {
                val m = mean.toDoubleOrNull() ?: 0.0
                val s = sd.toDoubleOrNull() ?: 1.0
                val cnt = n.toLongOrNull() ?: 30L
                val res = StatisticsCalculators.calculateConfidenceInterval(m, s, cnt, confLevel)
                onCalculate("Confidence Interval ($confLevel%)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Confidence Interval")
        }
    }
}

@Composable
private fun SequenceForm(onCalculate: (String, CalculationResult) -> Unit) {
    var type by remember { mutableStateOf("Arithmetic") }
    var a1 by remember { mutableStateOf("3") }
    var diffOrRatio by remember { mutableStateOf("5") }
    var n by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabRow(selectedTabIndex = if (type == "Arithmetic") 0 else 1) {
            Tab(selected = type == "Arithmetic", onClick = { type = "Arithmetic" }, text = { Text("Arithmetic (d)") })
            Tab(selected = type == "Geometric", onClick = { type = "Geometric" }, text = { Text("Geometric (r)") })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a1,
                onValueChange = { a1 = it },
                label = { Text("First Term (a₁)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = diffOrRatio,
                onValueChange = { diffOrRatio = it },
                label = { Text(if (type == "Arithmetic") "Difference (d)" else "Ratio (r)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = n,
                onValueChange = { n = it },
                label = { Text("Term n") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valA1 = a1.toDoubleOrNull() ?: 1.0
                val valDiff = diffOrRatio.toDoubleOrNull() ?: 1.0
                val valN = n.toLongOrNull() ?: 10L
                val res = StatisticsCalculators.calculateSequence(type, valA1, valDiff, valN)
                onCalculate("$type Sequence (n=$valN)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate nth Term & Sum Sn")
        }
    }
}

@Composable
private fun SampleSizeForm(onCalculate: (String, CalculationResult) -> Unit) {
    var confLevel by remember { mutableStateOf(95.0) }
    var moe by remember { mutableStateOf("5") }
    var pop by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = moe,
                onValueChange = { moe = it },
                label = { Text("Margin of Error (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = pop,
                onValueChange = { pop = it },
                label = { Text("Population (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(90.0 to "90%", 95.0 to "95%", 99.0 to "99%").forEach { (lvl, title) ->
                FilterChip(
                    selected = confLevel == lvl,
                    onClick = { confLevel = lvl },
                    label = { Text(title) }
                )
            }
        }
        Button(
            onClick = {
                val moeVal = moe.toDoubleOrNull() ?: 5.0
                val popVal = pop.toLongOrNull()
                val res = StatisticsCalculators.calculateSampleSize(confLevel, moeVal, popVal)
                onCalculate("Sample size for MOE $moeVal%", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Sample Size")
        }
    }
}

// -------------------------------------------------------------
// GEOMETRY FORMS
// -------------------------------------------------------------

@Composable
private fun PythagoreanForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a by remember { mutableStateOf("3") }
    var b by remember { mutableStateOf("4") }
    var c by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Formula: a² + b² = c² (Leave one empty to find)", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("Leg a") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = b,
                onValueChange = { b = it },
                label = { Text("Leg b") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = c,
                onValueChange = { c = it },
                label = { Text("Hypotenuse c") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valA = a.toDoubleOrNull()
                val valB = b.toDoubleOrNull()
                val valC = c.toDoubleOrNull()
                val res = GeometryCalculators.calculatePythagorean(valA, valB, valC)
                onCalculate("Pythagorean: a=$a, b=$b, c=$c", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Missing Side")
        }
    }
}

@Composable
private fun RightTriangleForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a by remember { mutableStateOf("6") }
    var b by remember { mutableStateOf("8") }
    var c by remember { mutableStateOf("") }
    var angleA by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Provide any 2 values (sides or angle)", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("Leg a") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = b,
                onValueChange = { b = it },
                label = { Text("Leg b") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = c,
                onValueChange = { c = it },
                label = { Text("Hypotenuse c") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = angleA,
                onValueChange = { angleA = it },
                label = { Text("Angle A (°)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valA = a.toDoubleOrNull()
                val valB = b.toDoubleOrNull()
                val valC = c.toDoubleOrNull()
                val valAngle = angleA.toDoubleOrNull()
                val res = GeometryCalculators.calculateRightTriangle(valA, valB, valC, valAngle)
                onCalculate("Right Triangle solve", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Right Triangle")
        }
    }
}

@Composable
private fun TriangleForm(onCalculate: (String, CalculationResult) -> Unit) {
    var a by remember { mutableStateOf("7") }
    var b by remember { mutableStateOf("8") }
    var c by remember { mutableStateOf("9") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Heron's Formula & All Angles (3 Sides)", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("Side a") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = b,
                onValueChange = { b = it },
                label = { Text("Side b") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = c,
                onValueChange = { c = it },
                label = { Text("Side c") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val valA = a.toDoubleOrNull() ?: 1.0
                val valB = b.toDoubleOrNull() ?: 1.0
                val valC = c.toDoubleOrNull() ?: 1.0
                val res = GeometryCalculators.calculateTriangle(valA, valB, valC)
                onCalculate("Triangle (a=$valA, b=$valB, c=$valC)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Triangle Properties")
        }
    }
}

@Composable
private fun CircleForm(onCalculate: (String, CalculationResult) -> Unit) {
    var type by remember { mutableStateOf("radius") }
    var value by remember { mutableStateOf("5") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("radius" to "Radius", "diameter" to "Diameter", "circumference" to "Circumference", "area" to "Area").forEach { (t, label) ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(label) }
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Known Value ($type)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                val v = value.toDoubleOrNull() ?: 1.0
                val res = GeometryCalculators.calculateCircle(type, v)
                onCalculate("Circle with $type = $v", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Circle")
        }
    }
}

@Composable
private fun Area2DForm(onCalculate: (String, CalculationResult) -> Unit) {
    var shape by remember { mutableStateOf("rectangle") }
    var p1 by remember { mutableStateOf("10") }
    var p2 by remember { mutableStateOf("5") }
    var p3 by remember { mutableStateOf("4") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("rectangle" to "Rectangle", "square" to "Square", "trapezoid" to "Trapezoid", "parallelogram" to "Parallelogram", "ellipse" to "Ellipse").forEach { (s, label) ->
                FilterChip(
                    selected = shape == s,
                    onClick = { shape = s },
                    label = { Text(label) }
                )
            }
        }

        when (shape) {
            "rectangle" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Length") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Width") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            "square" -> {
                OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Side Length") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            "trapezoid" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Base A") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Base B") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p3, onValueChange = { p3 = it }, label = { Text("Height") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            "parallelogram" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Base") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Height") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            "ellipse" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Semi-Major (a)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Semi-Minor (b)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        Button(
            onClick = {
                val v1 = p1.toDoubleOrNull() ?: 1.0
                val v2 = p2.toDoubleOrNull()
                val v3 = p3.toDoubleOrNull()
                val res = GeometryCalculators.calculateArea2D(shape, v1, v2, v3)
                onCalculate("Area of $shape", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Area & Perimeter")
        }
    }
}

@Composable
private fun VolumeForm(isSurfaceArea: Boolean, onCalculate: (String, CalculationResult) -> Unit) {
    var solid by remember { mutableStateOf("cube") }
    var p1 by remember { mutableStateOf("4") }
    var p2 by remember { mutableStateOf("6") }
    var p3 by remember { mutableStateOf("8") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("cube" to "Cube", "sphere" to "Sphere", "cylinder" to "Cylinder", "cone" to "Cone", "rectangular_prism" to "Rect Prism").forEach { (s, label) ->
                FilterChip(
                    selected = solid == s,
                    onClick = { solid = s },
                    label = { Text(label) }
                )
            }
        }

        when (solid) {
            "cube" -> {
                OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Side length (a)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            "sphere" -> {
                OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Radius (r)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            "cylinder", "cone" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Radius (r)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Height (h)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            "rectangular_prism" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Length") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Width") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = p3, onValueChange = { p3 = it }, label = { Text("Height") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        Button(
            onClick = {
                val v1 = p1.toDoubleOrNull() ?: 1.0
                val v2 = p2.toDoubleOrNull()
                val v3 = p3.toDoubleOrNull()
                val res = GeometryCalculators.calculateSolid3D(solid, v1, v2, v3)
                onCalculate("${if (isSurfaceArea) "Surface Area" else "Volume"} of $solid", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSurfaceArea) "Calculate Surface Area" else "Calculate Volume & Surface Area")
        }
    }
}

@Composable
private fun SlopeForm(onCalculate: (String, CalculationResult) -> Unit) {
    var x1 by remember { mutableStateOf("1") }
    var y1 by remember { mutableStateOf("2") }
    var x2 by remember { mutableStateOf("5") }
    var y2 by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(value = x1, onValueChange = { x1 = it }, label = { Text("x₁") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = y1, onValueChange = { y1 = it }, label = { Text("y₁") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(value = x2, onValueChange = { x2 = it }, label = { Text("x₂") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = y2, onValueChange = { y2 = it }, label = { Text("y₂") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Button(
            onClick = {
                val vx1 = x1.toDoubleOrNull() ?: 0.0
                val vy1 = y1.toDoubleOrNull() ?: 0.0
                val vx2 = x2.toDoubleOrNull() ?: 0.0
                val vy2 = y2.toDoubleOrNull() ?: 0.0
                val res = GeometryCalculators.calculateSlope(vx1, vy1, vx2, vy2)
                onCalculate("Slope from ($vx1,$vy1) to ($vx2,$vy2)", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Slope & Line Equation")
        }
    }
}

@Composable
private fun DistanceForm(onCalculate: (String, CalculationResult) -> Unit) {
    var is3D by remember { mutableStateOf(false) }
    var x1 by remember { mutableStateOf("0") }
    var y1 by remember { mutableStateOf("0") }
    var z1 by remember { mutableStateOf("0") }
    var x2 by remember { mutableStateOf("3") }
    var y2 by remember { mutableStateOf("4") }
    var z2 by remember { mutableStateOf("12") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(checked = is3D, onCheckedChange = { is3D = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (is3D) "3D Coordinates (X, Y, Z)" else "2D Coordinates (X, Y)")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = x1, onValueChange = { x1 = it }, label = { Text("x₁") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = y1, onValueChange = { y1 = it }, label = { Text("y₁") }, modifier = Modifier.weight(1f), singleLine = true)
            if (is3D) {
                OutlinedTextField(value = z1, onValueChange = { z1 = it }, label = { Text("z₁") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = x2, onValueChange = { x2 = it }, label = { Text("x₂") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = y2, onValueChange = { y2 = it }, label = { Text("y₂") }, modifier = Modifier.weight(1f), singleLine = true)
            if (is3D) {
                OutlinedTextField(value = z2, onValueChange = { z2 = it }, label = { Text("z₂") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        Button(
            onClick = {
                val vx1 = x1.toDoubleOrNull() ?: 0.0
                val vy1 = y1.toDoubleOrNull() ?: 0.0
                val vz1 = if (is3D) z1.toDoubleOrNull() else null
                val vx2 = x2.toDoubleOrNull() ?: 0.0
                val vy2 = y2.toDoubleOrNull() ?: 0.0
                val vz2 = if (is3D) z2.toDoubleOrNull() else null
                val res = GeometryCalculators.calculateDistance(vx1, vy1, vz1, vx2, vy2, vz2)
                onCalculate("Distance between points", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Euclidean Distance")
        }
    }
}

// -------------------------------------------------------------
// FINANCIAL FORMS
// -------------------------------------------------------------

@Composable
private fun LoanForm(onCalculate: (String, CalculationResult) -> Unit) {
    var principal by remember { mutableStateOf("100000") }
    var rate by remember { mutableStateOf("8.5") }
    var term by remember { mutableStateOf("20") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Loan / Mortgage / EMI Calculator", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = principal,
            onValueChange = { principal = it },
            label = { Text("Loan Amount (Principal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Annual Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                label = { Text("Term (Years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { principal = "500000"; rate = "7.5"; term = "30" }, label = { Text("Home Loan") })
            AssistChip(onClick = { principal = "50000"; rate = "12"; term = "5" }, label = { Text("Car Loan") })
            AssistChip(onClick = { principal = "20000"; rate = "15"; term = "2" }, label = { Text("Personal Loan") })
        }
        Button(
            onClick = {
                val p = principal.toDoubleOrNull() ?: 0.0
                val r = rate.toDoubleOrNull() ?: 0.0
                val t = term.toDoubleOrNull() ?: 0.0
                val res = ExtraCalculators.calculateLoan(p, r, t)
                onCalculate("Loan: $p at $r% for $t years", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Monthly EMI")
        }
    }
}

@Composable
private fun InterestForm(onCalculate: (String, CalculationResult) -> Unit) {
    var principal by remember { mutableStateOf("10000") }
    var rate by remember { mutableStateOf("8") }
    var years by remember { mutableStateOf("5") }
    var compounding by remember { mutableStateOf(12) } // 0 = simple

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = principal,
                onValueChange = { principal = it },
                label = { Text("Principal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Annual Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = years,
                onValueChange = { years = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Text("Compounding Frequency", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "Simple", 1 to "Yearly", 4 to "Quarterly", 12 to "Monthly", 365 to "Daily").forEach { (n, label) ->
                FilterChip(selected = compounding == n, onClick = { compounding = n }, label = { Text(label) })
            }
        }
        Button(
            onClick = {
                val p = principal.toDoubleOrNull() ?: 0.0
                val r = rate.toDoubleOrNull() ?: 0.0
                val y = years.toDoubleOrNull() ?: 0.0
                val typeLabel = when (compounding) {
                    0 -> "simple"
                    1 -> "annual"
                    4 -> "quarterly"
                    12 -> "monthly"
                    365 -> "daily"
                    else -> "compound"
                }
                val res = ExtraCalculators.calculateInterest(p, r, y, compounding)
                onCalculate("$typeLabel interest on $p at $r% for $y years", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Interest & Growth")
        }
    }
}

@Composable
private fun TipForm(onCalculate: (String, CalculationResult) -> Unit) {
    var bill by remember { mutableStateOf("1200") }
    var tipPct by remember { mutableStateOf("15") }
    var split by remember { mutableStateOf("2") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = bill,
            onValueChange = { bill = it },
            label = { Text("Total Bill Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tipPct,
                onValueChange = { tipPct = it },
                label = { Text("Tip (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = split,
                onValueChange = { split = it },
                label = { Text("Split (People)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("10" to "10%", "15" to "15%", "18" to "18%", "20" to "20%", "25" to "25%").forEach { (t, label) ->
                FilterChip(selected = tipPct == t, onClick = { tipPct = t }, label = { Text(label) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1", "2", "3", "4", "5").forEach { n ->
                FilterChip(selected = split == n, onClick = { split = n }, label = { Text("$n person${if (n != "1") "s" else ""}") })
            }
        }
        Button(
            onClick = {
                val b = bill.toDoubleOrNull() ?: 0.0
                val t = tipPct.toDoubleOrNull() ?: 0.0
                val s = split.toIntOrNull() ?: 1
                val res = ExtraCalculators.calculateTip(b, t, s)
                onCalculate("Tip $t% on bill $b split by $s", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Tip & Split")
        }
    }
}

@Composable
private fun InvestmentForm(onCalculate: (String, CalculationResult) -> Unit) {
    var start by remember { mutableStateOf("50000") }
    var monthly by remember { mutableStateOf("5000") }
    var rate by remember { mutableStateOf("12") }
    var years by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Compound Growth with Monthly Contributions", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = start,
                onValueChange = { start = it },
                label = { Text("Starting Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = monthly,
                onValueChange = { monthly = it },
                label = { Text("Monthly Addition") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Annual Return (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = years,
                onValueChange = { years = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { years = "5" }, label = { Text("5 years") })
            AssistChip(onClick = { years = "10" }, label = { Text("10 years") })
            AssistChip(onClick = { years = "20" }, label = { Text("20 years") })
            AssistChip(onClick = { years = "30" }, label = { Text("30 years") })
        }
        Button(
            onClick = {
                val s = start.toDoubleOrNull() ?: 0.0
                val m = monthly.toDoubleOrNull() ?: 0.0
                val r = rate.toDoubleOrNull() ?: 0.0
                val y = years.toDoubleOrNull() ?: 0.0
                val res = ExtraCalculators.calculateInvestment(s, m, r, y)
                onCalculate("Investment $s + $m/mo at $r% for $y years", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Investment Growth & ROI")
        }
    }
}

// -------------------------------------------------------------
// FITNESS & HEALTH FORMS
// -------------------------------------------------------------

@Composable
private fun BmiForm(onCalculate: (String, CalculationResult) -> Unit) {
    var weight by remember { mutableStateOf("70") }
    var height by remember { mutableStateOf("170") }
    var isImperial by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(checked = isImperial, onCheckedChange = { isImperial = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isImperial) "Imperial (lbs & inches)" else "Metric (kg & cm)")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text(if (isImperial) "Weight (lbs)" else "Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text(if (isImperial) "Height (inches)" else "Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Button(
            onClick = {
                val w = weight.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                // Convert imperial to metric if needed
                val weightKg = if (isImperial) w * 0.453592 else w
                val heightCm = if (isImperial) h * 2.54 else h
                val res = ExtraCalculators.calculateBmi(weightKg, heightCm)
                onCalculate("BMI for ${if (isImperial) "$w lbs, $h in" else "$w kg, $h cm"}", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate BMI")
        }
    }
}

@Composable
private fun CalorieBmrForm(onCalculate: (String, CalculationResult) -> Unit) {
    var gender by remember { mutableStateOf("male") }
    var weight by remember { mutableStateOf("75") }
    var height by remember { mutableStateOf("175") }
    var age by remember { mutableStateOf("30") }
    var activity by remember { mutableStateOf("moderate") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Gender tabs
        TabRow(selectedTabIndex = if (gender == "male") 0 else 1) {
            Tab(selected = gender == "male", onClick = { gender = "male" }, text = { Text("Male") })
            Tab(selected = gender == "female", onClick = { gender = "female" }, text = { Text("Female") })
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Text("Activity Level", style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "sedentary" to "Sedentary (little or no exercise)",
                "light" to "Light (1-3 days/week)",
                "moderate" to "Moderate (3-5 days/week)",
                "active" to "Active (6-7 days/week)",
                "very_active" to "Very Active (hard daily exercise)"
            ).forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = activity == key, onClick = { activity = key })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(
            onClick = {
                val w = weight.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                val a = age.toIntOrNull() ?: 0
                val res = ExtraCalculators.calculateBmr(gender, w, h, a, activity)
                onCalculate("BMR/TDEE for $gender, $w kg, $h cm, age $a", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate BMR & Daily Calories (TDEE)")
        }
    }
}

@Composable
private fun BodyFatForm(onCalculate: (String, CalculationResult) -> Unit) {
    var gender by remember { mutableStateOf("male") }
    var height by remember { mutableStateOf("175") }
    var waist by remember { mutableStateOf("85") }
    var neck by remember { mutableStateOf("38") }
    var hip by remember { mutableStateOf("95") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabRow(selectedTabIndex = if (gender == "male") 0 else 1) {
            Tab(selected = gender == "male", onClick = { gender = "male" }, text = { Text("Male") })
            Tab(selected = gender == "female", onClick = { gender = "female" }, text = { Text("Female") })
        }

        Text("All measurements in centimeters (cm)", style = MaterialTheme.typography.labelMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = neck,
                onValueChange = { neck = it },
                label = { Text("Neck (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = waist,
                onValueChange = { waist = it },
                label = { Text("Waist (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (gender == "female") {
                OutlinedTextField(
                    value = hip,
                    onValueChange = { hip = it },
                    label = { Text("Hip (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Button(
            onClick = {
                val h = height.toDoubleOrNull() ?: 0.0
                val w = waist.toDoubleOrNull() ?: 0.0
                val n = neck.toDoubleOrNull() ?: 0.0
                val p = if (gender == "female") hip.toDoubleOrNull() else null
                val res = ExtraCalculators.calculateBodyFat(gender, h, w, n, p)
                onCalculate("Body fat ($gender): height $h, waist $w, neck $n", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Body Fat %")
        }
    }
}

// -------------------------------------------------------------
// OTHER FORMS
// -------------------------------------------------------------

@Composable
private fun AgeForm(onCalculate: (String, CalculationResult) -> Unit) {
    var year by remember { mutableStateOf("1995") }
    var month by remember { mutableStateOf("6") }
    var day by remember { mutableStateOf("15") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enter your Date of Birth", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = day,
                onValueChange = { day = it },
                label = { Text("Day") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = month,
                onValueChange = { month = it },
                label = { Text("Month") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Year") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(2f),
                singleLine = true
            )
        }
        Button(
            onClick = {
                val y = year.toIntOrNull() ?: 2000
                val m = month.toIntOrNull() ?: 1
                val d = day.toIntOrNull() ?: 1
                val res = ExtraCalculators.calculateAge(y, m, d)
                onCalculate("Age from $d/$m/$y", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Exact Age")
        }
    }
}

@Composable
private fun GpaForm(onCalculate: (String, CalculationResult) -> Unit) {
    // Store pairs of (grade letter → grade point, credit hours)
    var courses by remember {
        mutableStateOf(
            listOf(
                Triple("A", "4.0", "3"),
                Triple("B+", "3.3", "4"),
                Triple("B", "3.0", "3"),
                Triple("A-", "3.7", "3")
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enter Grade Points & Credit Hours per course", style = MaterialTheme.typography.labelMedium)

        courses.forEachIndexed { idx, (_, grade, credit) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Course ${idx + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp))
                OutlinedTextField(
                    value = grade,
                    onValueChange = { newGrade ->
                        courses = courses.toMutableList().also { it[idx] = Triple(courses[idx].first, newGrade, courses[idx].third) }
                    },
                    label = { Text("GP (0-4)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = credit,
                    onValueChange = { newCredit ->
                        courses = courses.toMutableList().also { it[idx] = Triple(courses[idx].first, courses[idx].second, newCredit) }
                    },
                    label = { Text("Credits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                TextButton(onClick = {
                    courses = courses.toMutableList().also { it.removeAt(idx) }
                }) {
                    Text("✕")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { courses = courses + Triple("", "4.0", "3") },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Add Course")
            }

            // Quick grade reference chips
            AssistChip(onClick = {}, label = { Text("A=4.0 B=3.0 C=2.0 D=1.0") })
        }

        Button(
            onClick = {
                val pairs = courses.mapNotNull { (_, gp, cr) ->
                    val g = gp.toDoubleOrNull()
                    val c = cr.toDoubleOrNull()
                    if (g != null && c != null && c > 0) Pair(g, c) else null
                }
                val res = ExtraCalculators.calculateGpa(pairs)
                onCalculate("GPA from ${pairs.size} courses", res)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Cumulative GPA")
        }
    }
}
