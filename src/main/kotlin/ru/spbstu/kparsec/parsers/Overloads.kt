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
    val skippedBefore: Parser<Char, Any?> get() = Literals.SPACES
    val skippedAfter: Parser<Char, Any?> get() = skippedBefore

    operator fun String.unaryPlus(): Parser<Char, String> = -skippedBefore + constant(this) + -skippedAfter
    operator fun Char.unaryPlus(): Parser<Char, Char> = -skippedBefore + char(this) + -skippedAfter
    operator fun CharRange.unaryPlus(): Parser<Char, Char> = -skippedBefore + range(this) + -skippedAfter
    operator fun String.unaryMinus(): Parser<Char, Unit> = -+this
    operator fun Char.unaryMinus(): Parser<Char, Unit> = -+this
    operator fun CharRange.unaryMinus(): Parser<Char, Unit> = -+this

    fun <A> lexeme(self: Parser<Char, A>): Parser<Char, A> = -skippedBefore + self + -skippedAfter
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
fun StringsAsParsers(ignored: Parser<Char, Any?> = Literals.SPACES): StringsAsParsers = object : StringsAsParsers{
    override val skippedBefore = ignored
}
