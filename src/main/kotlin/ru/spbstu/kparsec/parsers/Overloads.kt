package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Parser

/**
 * A convenient way of using string and char literals as [Parser], wrapped into an interface to avoid
 * global namespace pollution.
 * Example:
 * ```kotlin
 *   class ParensParser: StringsAsParsers {
 *       val inner: Parser<Char, Unit> = defer { parens }
 *       val parens: Parser<Char, Unit> = -'(' + inner + -')'
 *   }
 * ```
 */
interface StringsAsParsers {
    operator fun String.unaryPlus(): Parser<Char, String> = Literals.lexeme(this)
    operator fun Char.unaryPlus(): Parser<Char, Char> = Literals.lexeme(this)
    operator fun CharRange.unaryPlus(): Parser<Char, Char> = Literals.lexeme(range(this))
    operator fun String.unaryMinus(): Parser<Char, Unit> = -+this
    operator fun Char.unaryMinus(): Parser<Char, Unit> = -+this
    operator fun CharRange.unaryMinus(): Parser<Char, Unit> = -+this
}

/**
 * A convenient way of using string and char literals as [Parser], wrapped into a function to avoid
 * global namespace pollution.
 * Example:
 * ```kotlin
 *   with(StringsAsParsers()) {
 *       val parens: Parser<Char, Unit> = recursive {
 *          -'(' + it + -')'
 *       }
 *   }
 * ```
 */
fun StringsAsParsers(): StringsAsParsers = object : StringsAsParsers{}
