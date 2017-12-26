package ru.spbstu.kparsec.parsers

interface StringsAsParsers {
    operator fun String.unaryPlus() = Literals.lexeme(this)
    operator fun Char.unaryPlus() = Literals.lexeme(this)
    operator fun CharRange.unaryPlus() = Literals.lexeme(range(this))
    operator fun String.unaryMinus() = -+this
    operator fun Char.unaryMinus() = -+this
    operator fun CharRange.unaryMinus() = -+this
}

fun StringsAsParsers() = object : StringsAsParsers{}
