package com.allcalc.app.domain.calculator

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

class EvaluationException(message: String) : Exception(message)

sealed interface EvalResult {
    data class Value(val value: BigDecimal, val steps: List<String>) : EvalResult
    data class Solved(val variable: Char, val value: BigDecimal, val steps: List<String>) : EvalResult
    data class Equality(val lhs: BigDecimal, val rhs: BigDecimal, val holds: Boolean, val steps: List<String>) : EvalResult
}

sealed class Node {
    data class Num(val value: BigDecimal) : Node()
    data class Const(val name: String, val value: BigDecimal) : Node()
    data class Var(val name: Char) : Node()
    data class Bin(val op: String, val left: Node, val right: Node) : Node()
    data class Unary(val op: String, val operand: Node) : Node()
    data class Post(val op: String, val operand: Node) : Node()
    data class Pow(val base: Node, val exponent: Node) : Node()
    data class FuncCall(val name: String, val arg: Node) : Node()
    data class Eq(val lhs: Node, val rhs: Node) : Node()
}

class ExpressionEngine(private val defaultPrecision: Int = 12) {

    private companion object {
        val HUNDRED: BigDecimal = BigDecimal.valueOf(100)
        const val TRIG_EPSILON = 1e-12
    }

    private enum class K { NUM, OP, LP, RP, POST, FUNC, CONST, VAR, BAR, END }

    private data class Tok(val kind: K, val value: String, val pos: Int) {
        val big: BigDecimal get() = BigDecimal(value)
    }

    private val endToken = Tok(K.END, "", -1)

    private val functionNames = setOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "abs")

    fun autocomplete(input: String): String {
        var open = 0
        var bars = 0
        for (ch in input) {
            when (ch) {
                '(' -> open++
                ')' -> if (open > 0) open--
                '|' -> bars++
            }
        }
        val sb = StringBuilder(input)
        repeat(open) { sb.append(')') }
        if (bars % 2 == 1) sb.append('|')
        return sb.toString()
    }

    fun evaluate(
        input: String,
        angleModeDeg: Boolean = true,
        precision: Int = defaultPrecision,
        allowVariables: Boolean = false
    ): EvalResult {
        if (input.isBlank()) throw EvaluationException("Enter an expression")
        val toks = tokenize(input)
        val root = Parser(toks, allowVariables).parseStatement()
        val mc = MathContext(precision)
        return when (root) {
            is Node.Eq -> solveEquation(root, angleModeDeg, mc)
            else -> {
                val value = eval(root, emptyMap(), angleModeDeg, mc)
                EvalResult.Value(value, buildSteps(root, emptyMap(), angleModeDeg, mc))
            }
        }
    }

    private fun tokenize(source: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        val n = source.length
        fun isDigit(c: Char) = c in '0'..'9'
        while (i < n) {
            val c = source[i]
            when {
                c.isWhitespace() -> i++
                isDigit(c) || (c == '.' && i + 1 < n && isDigit(source[i + 1])) -> {
                    val start = i
                    while (i < n && isDigit(source[i])) i++
                    if (i < n && source[i] == '.') {
                        i++
                        while (i < n && isDigit(source[i])) i++
                    }
                    if (i < n && (source[i] == 'E' || source[i] == 'e')) {
                        val j = i + 1
                        val hasExponent = (j < n && isDigit(source[j])) ||
                            (j < n && (source[j] == '+' || source[j] == '-') && j + 1 < n && isDigit(source[j + 1]))
                        if (hasExponent) {
                            i = j
                            if (source[i] == '+' || source[i] == '-') i++
                            while (i < n && isDigit(source[i])) i++
                        }
                    }
                    out += Tok(K.NUM, source.substring(start, i), start)
                }
                c == '+' -> { out += Tok(K.OP, "+", i); i++ }
                c == '-' || c == '−' || c == '–' || c == '—' -> { out += Tok(K.OP, "-", i); i++ }
                c == '×' || c == '*' || c == '·' -> { out += Tok(K.OP, "*", i); i++ }
                c == '÷' || c == '/' || c == '∕' -> { out += Tok(K.OP, "/", i); i++ }
                c == '%' -> { out += Tok(K.OP, "%", i); i++ }
                c == '^' -> { out += Tok(K.OP, "^", i); i++ }
                c == '=' -> { out += Tok(K.OP, "=", i); i++ }
                c == '(' || c == '（' -> { out += Tok(K.LP, "(", i); i++ }
                c == ')' || c == '）' -> { out += Tok(K.RP, ")", i); i++ }
                c == '!' -> { out += Tok(K.POST, "!", i); i++ }
                c == '|' -> { out += Tok(K.BAR, "|", i); i++ }
                c == 'π' -> { out += Tok(K.CONST, "π", i); i++ }
                c == '√' -> { out += Tok(K.FUNC, "sqrt", i); i++ }
                c == ',' -> i++
                c.isLetter() -> {
                    val start = i
                    while (i < n && source[i].isLetter()) i++
                    val word = source.substring(start, i)
                    val lower = word.lowercase()
                    when {
                        functionNames.contains(lower) -> out += Tok(K.FUNC, lower, start)
                        lower == "pi" -> out += Tok(K.CONST, "π", start)
                        lower == "e" -> out += Tok(K.CONST, "e", start)
                        lower == "of" -> out += Tok(K.OP, "*", start)
                        word.length == 1 -> out += Tok(K.VAR, lower, start)
                        else -> throw EvaluationException("Unknown identifier '$word'")
                    }
                }
                c == '.' -> throw EvaluationException("Invalid '.' in an expression")
                else -> throw EvaluationException("Invalid character '$c'")
            }
        }
        return out
    }

    private inner class Parser(private val toks: List<Tok>, private val allowVariables: Boolean) {
        private var idx = 0

        private fun peek(): Tok = toks.getOrElse(idx) { endToken }
        private fun next(): Tok = toks.getOrElse(idx++) { endToken }
        private fun expect(kind: K, what: String): Tok {
            val t = peek()
            if (t.kind != kind) throw EvaluationException("Expected $what")
            idx++
            return t
        }

        private fun operandStart(t: Tok): Boolean = when (t.kind) {
            K.NUM, K.LP, K.FUNC, K.CONST, K.VAR -> true
            else -> false
        }

        fun parseStatement(): Node {
            if (toks.any { it.kind == K.OP && it.value == "=" }) {
                val lhs = parseLhs()
                val eq = peek()
                if (eq.kind != K.OP || eq.value != "=") throw EvaluationException("Expected '='")
                idx++
                val rhs = parseAdditive()
                if (peek().kind != K.END) throw EvaluationException("Unexpected input after the equation")
                return Node.Eq(lhs, rhs)
            }
            val node = parseAdditive()
            if (peek().kind != K.END) throw EvaluationException("Unexpected token '${peek().value}'")
            return node
        }

        private fun parseLhs(): Node {
            var node = parseTerm()
            while (true) {
                val t = peek()
                if (t.kind == K.END || (t.kind == K.OP && t.value == "=")) break
                if (t.kind == K.OP && (t.value == "+" || t.value == "-")) {
                    next()
                    node = additiveNode(t.value, node, parseTerm())
                } else break
            }
            return node
        }

        private fun parseAdditive(): Node {
            var node = parseTerm()
            while (true) {
                val t = peek()
                if (t.kind == K.OP && (t.value == "+" || t.value == "-")) {
                    next()
                    node = additiveNode(t.value, node, parseTerm())
                } else break
            }
            return node
        }

        /**
         * Calculator-style percent: "200 + 10%" means "200 plus 10% of 200" (= 220), while a
         * bare "10%" is still 0.1 and "200 × 10%" is still 20.
         */
        private fun additiveNode(op: String, left: Node, right: Node): Node =
            if (right is Node.Post && right.op == "%") {
                Node.Bin(if (op == "+") "pct+" else "pct-", left, right.operand)
            } else {
                Node.Bin(op, left, right)
            }

        private fun parseTerm(): Node {
            var node = parseUnary()
            while (true) {
                val t = peek()
                when {
                    t.kind == K.OP && (t.value == "*" || t.value == "/") -> {
                        next()
                        node = Node.Bin(t.value, node, parseUnary())
                    }
                    operandStart(t) -> node = Node.Bin("*", node, parseUnary())
                    else -> return node
                }
            }
        }

        private fun parseUnary(): Node {
            val t = peek()
            if (t.kind == K.OP && t.value == "-") {
                next()
                return Node.Unary("-", parseUnary())
            }
            if (t.kind == K.OP && t.value == "+") {
                next()
                return parseUnary()
            }
            return parsePower()
        }

        private fun parsePower(): Node {
            val base = parsePostfix()
            val t = peek()
            if (t.kind == K.OP && t.value == "^") {
                next()
                return Node.Pow(base, parseUnary())
            }
            return base
        }

        private fun parsePostfix(): Node {
            var node = parsePrimary()
            while (true) {
                val t = peek()
                when {
                    t.kind == K.POST && t.value == "!" -> {
                        next()
                        node = Node.Post("!", node)
                    }
                    t.kind == K.OP && t.value == "%" -> {
                        next()
                        node = Node.Post("%", node)
                    }
                    else -> return node
                }
            }
        }

        private fun parsePrimary(): Node {
            val t = peek()
            return when (t.kind) {
                K.NUM -> { next(); Node.Num(t.big) }
                K.CONST -> {
                    next()
                    if (t.value == "π") Node.Const("π", BigDecimal.valueOf(Math.PI))
                    else Node.Const("e", BigDecimal.valueOf(Math.E))
                }
                K.VAR -> {
                    next()
                    if (!allowVariables) throw EvaluationException("Unknown symbol '${t.value.uppercase()}'")
                    Node.Var(t.value.first())
                }
                K.LP -> {
                    next()
                    val inner = parseAdditive()
                    expect(K.RP, "')'")
                    inner
                }
                K.BAR -> {
                    next()
                    val inner = parseAdditive()
                    expect(K.BAR, "'|'")
                    Node.FuncCall("abs", inner)
                }
                K.FUNC -> {
                    next()
                    parseFunctionCall(t.value)
                }
                else -> throw EvaluationException(
                    if (t.kind == K.END) "Unexpected end of expression" else "Unexpected token '${t.value}'"
                )
            }
        }

        private fun parseFunctionCall(name: String): Node {
            if (name == "sqrt") {
                if (peek().kind != K.LP) return Node.FuncCall("sqrt", parseUnary())
                next()
                val inner = parseAdditive()
                expect(K.RP, "')'")
                return Node.FuncCall("sqrt", inner)
            }
            expect(K.LP, "'('")
            val inner = parseAdditive()
            expect(K.RP, "')'")
            return Node.FuncCall(name, inner)
        }
    }

    private fun eval(node: Node, vars: Map<Char, Double>, angleDeg: Boolean, mc: MathContext): BigDecimal =
        when (node) {
            is Node.Num -> node.value
            is Node.Const -> node.value
            is Node.Var -> {
                val v = vars[node.name]
                    ?: throw EvaluationException("Variable '${node.name}' has no value")
                BigDecimal.valueOf(v)
            }
            is Node.Unary -> {
                val v = eval(node.operand, vars, angleDeg, mc)
                if (node.op == "-") v.negate(mc) else v
            }
            is Node.Post -> when (node.op) {
                "%" -> eval(node.operand, vars, angleDeg, mc).divide(BigDecimal.valueOf(100), mc)
                "!" -> factorial(eval(node.operand, vars, angleDeg, mc))
                else -> throw EvaluationException("Unknown postfix operator '${node.op}'")
            }
            is Node.Bin -> binaryOp(
                node.op,
                eval(node.left, vars, angleDeg, mc),
                eval(node.right, vars, angleDeg, mc),
                mc
            )
            is Node.Pow -> power(
                eval(node.base, vars, angleDeg, mc),
                eval(node.exponent, vars, angleDeg, mc),
                mc
            )
            is Node.FuncCall -> function(node.name, eval(node.arg, vars, angleDeg, mc), angleDeg, mc)
            is Node.Eq -> throw EvaluationException("Invalid equation")
        }

    private fun binaryOp(op: String, a: BigDecimal, b: BigDecimal, mc: MathContext): BigDecimal = when (op) {
        "+" -> a.add(b, mc)
        "-" -> a.subtract(b, mc)
        "*" -> a.multiply(b, mc)
        "pct+" -> a.add(a.multiply(b, mc).divide(HUNDRED, mc), mc)
        "pct-" -> a.subtract(a.multiply(b, mc).divide(HUNDRED, mc), mc)
        "/" -> {
            if (b.compareTo(BigDecimal.ZERO) == 0) throw EvaluationException("Cannot divide by zero")
            a.divide(b, mc)
        }
        else -> throw EvaluationException("Unknown operator '$op'")
    }

    private fun power(base: BigDecimal, exponent: BigDecimal, mc: MathContext): BigDecimal {
        val expBig = try {
            exponent.toBigIntegerExact()
        } catch (e: ArithmeticException) {
            null
        }
        val intExp = if (expBig != null) {
            if (expBig > BigInteger.valueOf(Int.MAX_VALUE.toLong()) || expBig < BigInteger.valueOf(Int.MIN_VALUE.toLong())) {
                throw EvaluationException("Result too large")
            } else expBig.toInt()
        } else null
        if (intExp != null) {
            if (abs(intExp.toLong()) > 4096) throw EvaluationException("Result too large")
            return if (intExp >= 0) {
                base.pow(intExp, mc)
            } else {
                if (base.compareTo(BigDecimal.ZERO) == 0) throw EvaluationException("Cannot divide by zero")
                BigDecimal.ONE.divide(base.pow(-intExp), mc)
            }
        }
        val db = base.toDouble()
        val de = exponent.toDouble()
        if (db < 0.0) throw EvaluationException("Invalid power of a negative number")
        val v = Math.pow(db, de)
        if (v.isNaN() || v.isInfinite()) throw EvaluationException("Result too large or undefined")
        return BigDecimal.valueOf(v)
    }

    private fun factorial(x: BigDecimal): BigDecimal {
        if (x.compareTo(BigDecimal.ZERO) < 0) throw EvaluationException("Factorial is not defined for negative numbers")
        val whole = try {
            x.toBigIntegerExact()
        } catch (e: ArithmeticException) {
            throw EvaluationException("Factorial requires a whole number")
        }
        val n = if (whole > BigInteger.valueOf(170)) {
            throw EvaluationException("Factorial is too large to display")
        } else whole.toInt()
        var r = 1.0
        for (i in 2..n) r *= i
        if (r.isInfinite()) throw EvaluationException("Factorial is too large to display")
        return BigDecimal.valueOf(r)
    }

    private fun function(name: String, x: BigDecimal, angleDeg: Boolean, mc: MathContext): BigDecimal {
        fun toRadians(v: Double) = if (angleDeg) v * Math.PI / 180.0 else v
        fun fromRadians(v: Double) = if (angleDeg) v * 180.0 / Math.PI else v
        // Values like sin(180°) come out as 1.2E-16 in floating point; show a clean 0 instead.
        fun cleanTrig(v: Double) = if (v.isFinite() && abs(v) < TRIG_EPSILON) 0.0 else v
        val d = x.toDouble()
        val rad = toRadians(d)
        val v: Double = when (name) {
            "sin" -> cleanTrig(Math.sin(rad))
            "cos" -> cleanTrig(Math.cos(rad))
            "tan" -> {
                if (abs(Math.cos(rad)) < TRIG_EPSILON) {
                    throw EvaluationException("tan is undefined for this angle (cosine is 0)")
                }
                Math.tan(rad)
            }
            "asin" -> {
                if (abs(d) > 1.0) throw EvaluationException("asin is only defined for values between −1 and 1")
                fromRadians(Math.asin(d))
            }
            "acos" -> {
                if (abs(d) > 1.0) throw EvaluationException("acos is only defined for values between −1 and 1")
                fromRadians(Math.acos(d))
            }
            "atan" -> fromRadians(Math.atan(d))
            "ln" -> {
                if (x.signum() <= 0) throw EvaluationException("ln is undefined for numbers that are 0 or negative")
                Math.log(d)
            }
            "log" -> {
                if (x.signum() <= 0) throw EvaluationException("log is undefined for numbers that are 0 or negative")
                Math.log10(d)
            }
            "sqrt" -> {
                if (x.signum() < 0) throw EvaluationException("Square root of a negative number is undefined")
                Math.sqrt(d)
            }
            "abs" -> abs(d)
            else -> throw EvaluationException("Unknown function '$name'")
        }
        if (v.isNaN() || v.isInfinite()) throw EvaluationException("Result is undefined or too large")
        return BigDecimal.valueOf(v)
    }

    private fun solveEquation(eq: Node.Eq, angleDeg: Boolean, mc: MathContext): EvalResult {
        val vars = mutableSetOf<Char>()
        collectVars(eq, vars)

        if (vars.isEmpty()) {
            val lhs = eval(eq.lhs, emptyMap(), angleDeg, mc)
            val rhs = eval(eq.rhs, emptyMap(), angleDeg, mc)
            val holds = lhs.subtract(rhs, mc).abs().toDouble() < 1e-9
            val steps = listOf(
                "Left side = ${formatSmart(lhs, mc.precision)}",
                "Right side = ${formatSmart(rhs, mc.precision)}",
                if (holds) "Both sides are equal" else "The two sides are not equal"
            )
            return EvalResult.Equality(lhs, rhs, holds, steps)
        }

        if (vars.size > 1) {
            val names = vars.sorted().joinToString(", ") { it.toString() }
            throw EvaluationException("Only equations with a single variable are supported (found: $names)")
        }

        val v = vars.first()
        fun f(x: Double): Double {
            val map = mapOf(v to x)
            return eval(eq.lhs, map, angleDeg, mc).toDouble() - eval(eq.rhs, map, angleDeg, mc).toDouble()
        }

        val f0 = f(0.0)
        val f1 = f(1.0)
        val f2 = f(2.0)
        val a = f1 - f0
        val scale = maxOf(1.0, abs(f0), abs(f1), abs(f2))
        if (abs((f2 - f1) - a) > 1e-6 * scale) throw EvaluationException("Only linear equations are supported")
        if (abs(a) < 1e-12 * scale) {
            if (abs(f0) < 1e-9) throw EvaluationException("This equation is true for every real number")
            throw EvaluationException("This equation has no real solution")
        }
        val solution = -f0 / a
        val steps = listOf(
            "Equation: ${expr(eq.lhs)} = ${expr(eq.rhs)}",
            "Define f($v) = left side − right side and solve f($v) = 0.",
            "f(0) = ${formatSmart(BigDecimal.valueOf(f0), mc.precision)}",
            "f(1) = ${formatSmart(BigDecimal.valueOf(f1), mc.precision)}",
            "$v = −f(0) ÷ (f(1) − f(0)) = ${formatSmart(BigDecimal.valueOf(solution), mc.precision)}"
        )
        return EvalResult.Solved(v, BigDecimal.valueOf(solution), steps)
    }

    private fun collectVars(node: Node, out: MutableSet<Char>) {
        when (node) {
            is Node.Var -> out.add(node.name)
            is Node.Num, is Node.Const -> Unit
            is Node.Unary -> collectVars(node.operand, out)
            is Node.Post -> collectVars(node.operand, out)
            is Node.Pow -> {
                collectVars(node.base, out)
                collectVars(node.exponent, out)
            }
            is Node.FuncCall -> collectVars(node.arg, out)
            is Node.Bin -> {
                collectVars(node.left, out)
                collectVars(node.right, out)
            }
            is Node.Eq -> {
                collectVars(node.lhs, out)
                collectVars(node.rhs, out)
            }
        }
    }

    private fun size(node: Node): Int = when (node) {
        is Node.Num, is Node.Const, is Node.Var -> 1
        is Node.Unary -> 1 + size(node.operand)
        is Node.Post -> 1 + size(node.operand)
        is Node.Pow -> 1 + size(node.base) + size(node.exponent)
        is Node.FuncCall -> 1 + size(node.arg)
        is Node.Bin -> 1 + size(node.left) + size(node.right)
        is Node.Eq -> 1 + size(node.lhs) + size(node.rhs)
    }

    private fun buildSteps(
        root: Node,
        vars: Map<Char, Double>,
        angleDeg: Boolean,
        mc: MathContext
    ): List<String> {
        if (size(root) > 24) return emptyList()
        val out = mutableListOf<String>()

        fun rec(node: Node): BigDecimal = when (node) {
            is Node.Num -> node.value
            is Node.Const -> node.value
            is Node.Var -> BigDecimal.valueOf(vars[node.name] ?: 0.0)
            is Node.Unary -> {
                val x = rec(node.operand)
                val r = if (node.op == "-") x.negate(mc) else x
                out += "-${expr(node.operand)} = ${formatSmart(r, mc.precision)}"
                r
            }
            is Node.Post -> {
                val x = rec(node.operand)
                val r = if (node.op == "%") x.divide(HUNDRED, mc) else factorial(x)
                out += "${expr(node.operand)}${node.op} = ${formatSmart(r, mc.precision)}"
                r
            }
            is Node.Bin -> {
                rec(node.left)
                rec(node.right)
                val res = binaryOp(
                    node.op,
                    eval(node.left, vars, angleDeg, mc),
                    eval(node.right, vars, angleDeg, mc),
                    mc
                )
                val opDisplay = when (node.op) {
                    "*" -> "×"
                    "/" -> "÷"
                    "pct+" -> "+"
                    "pct-" -> "-"
                    else -> node.op
                }
                val rightText = if (node.op == "pct+" || node.op == "pct-") {
                    "${expr(node.right)}%"
                } else {
                    expr(node.right)
                }
                out += "${expr(node.left)} $opDisplay $rightText = ${formatSmart(res, mc.precision)}"
                res
            }
            is Node.Pow -> {
                val b = rec(node.base)
                val e = rec(node.exponent)
                val r = power(b, e, mc)
                out += "${expr(node.base)} ^ ${expr(node.exponent)} = ${formatSmart(r, mc.precision)}"
                r
            }
            is Node.FuncCall -> {
                val x = rec(node.arg)
                val r = function(node.name, x, angleDeg, mc)
                if (node.name != "abs") out += "${node.name}(${expr(node.arg)}) = ${formatSmart(r, mc.precision)}"
                r
            }
            is Node.Eq -> throw EvaluationException("Invalid equation")
        }

        rec(root)
        return out
    }

    private fun expr(node: Node): String = when (node) {
        is Node.Num -> node.value.stripTrailingZeros().toPlainString()
        is Node.Const -> node.name
        is Node.Var -> node.name.toString()
        is Node.Unary -> if (node.op == "-") "-${expr(node.operand)}" else expr(node.operand)
        is Node.Post -> "${expr(node.operand)}${node.op}"
        is Node.Pow -> "(${expr(node.base)}^${expr(node.exponent)})"
        is Node.FuncCall -> "${node.name}(${expr(node.arg)})"
        is Node.Bin -> {
            val op = when (node.op) {
                "*" -> " × "
                "/" -> " ÷ "
                "pct+" -> " + "
                "pct-" -> " - "
                else -> " ${node.op} "
            }
            val rightText = if (node.op == "pct+" || node.op == "pct-") {
                "${expr(node.right)}%"
            } else {
                expr(node.right)
            }
            "(${expr(node.left)}$op$rightText)"
        }
        is Node.Eq -> "${expr(node.lhs)} = ${expr(node.rhs)}"
    }
}

fun formatSmart(value: BigDecimal, precision: Int): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    val rounded = value.round(MathContext(precision)).stripTrailingZeros()
    val d = rounded.toDouble()
    return if (abs(d) >= 1e12 || (abs(d) > 0 && abs(d) < 1e-9)) {
        rounded.toString()
    } else {
        rounded.toPlainString()
    }
}