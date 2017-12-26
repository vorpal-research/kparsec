package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*
import ru.spbstu.kparsec.parsers.Literals.lexeme

private fun List<Double>.product() = foldRight(1.0){ a, b -> a * b }

object CalculatorParser {

    val atom: Parser<Char, Double> = Literals.FLOAT or (-lexeme('(') + defer { expr } + -lexeme(')'))

    val mult_ = atom joinedBy -lexeme('*')
    val mult = mult_.map { it.product() }

    val sum_ = mult + (-lexeme('+') + mult).many()
    val sum = sum_.map { it.sum() }

    val expr = sum
    val whole = expr + eof()
}
