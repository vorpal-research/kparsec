package ru.spbstu.kparsec.examples

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

internal fun factorial(x: Int) = (1..x).fold(1){ a, b -> a * b }.toDouble()

object ComplexCalculatorParser: StringsAsParsers, DelegateParser<Char, Double> {

    val literal= Literals.FLOAT
    val parens = (-'(' + defer { expr } + -')')
    val atom= literal or parens

    fun function(name: String, body: (Double) -> Double) = -name + parens.map(body)

    val call: Parser<Char, Double> =
            function("sin", ::sin) or
                    function("cos", ::cos) or
                    function("sqrt", ::sqrt) or
                    atom

    val op: Parser<Char, Double> = operatorTable(call) {
        (-'!')(priority = 6, assoc = Assoc.POSTFIX){ x -> factorial(round(x).toInt()) }
        (-'-')(priority = 6, assoc = Assoc.PREFIX){ x -> -x }
        (-'+')(priority = 2){ a, b -> a + b }
        (-'-')(priority = 2){ a, b -> a - b }
        (-'*')(priority = 4){ a, b -> a * b }
        (-'/')(priority = 4){ a, b -> a / b }
    }

    val expr: Parser<Char, Double> = op

    override val self: Parser<Char, Double> = expr + eof()
}
