package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Parser

interface StringsAsParsers {
    operator fun String.unaryPlus(): Parser<Char, String> = Literals.lexeme(this)
    operator fun Char.unaryPlus(): Parser<Char, Char> = Literals.lexeme(this)
    operator fun CharRange.unaryPlus(): Parser<Char, Char> = Literals.lexeme(range(this))
    operator fun String.unaryMinus(): Parser<Char, Unit> = -+this
    operator fun Char.unaryMinus(): Parser<Char, Unit> = -+this
    operator fun CharRange.unaryMinus(): Parser<Char, Unit> = -+this
}

fun StringsAsParsers(): StringsAsParsers = object : StringsAsParsers{}
