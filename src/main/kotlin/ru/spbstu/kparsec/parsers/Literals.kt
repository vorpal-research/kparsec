package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*

object Literals {
    val SPACES = regex("\\s*".toRegex()).asParser()

    fun<T> lexeme(inner: Parser<Char, T>) = -SPACES + inner + -SPACES
    fun lexeme(inner: String) = lexeme(constant(inner))
    fun lexeme(inner: Char) = lexeme(char(inner))

    val FLOAT: Parser<Char, Double> = regex("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?".toRegex()).map { it.toDouble() } named "Float"
    val DECIMAL: Parser<Char, Long> = regex("[0-9]+".toRegex()).map { it.toLong() } named "Decimal"
    val OCTAL: Parser<Char, Long> = regex("[0-7]+".toRegex()).map { it.toLong(8) } named "Octal"
    val HEXADECIMAL: Parser<Char, Long> = regex("[0-9a-fA-F]+".toRegex()).map { it.toLong(16) } named "Hexadecimal"
    val BOOLEAN: Parser<Char, Boolean> = constant("true").map { true } or constant("false").map { false } named "Boolean"

    val CINTEGER: Parser<Char, Long> = (-char('0') + ((-char('x') + HEXADECIMAL) or OCTAL)) or DECIMAL named "C Integer"

    val OCT_DIGIT: Parser<Char, Int> = range('0'..'7').map { it - '0' }
    val DEC_DIGIT: Parser<Char, Int> = range('0'..'9').map { it - '0' }
    val HEX_DIGIT = DEC_DIGIT or range('A'..'F').map { it - 'A' + 10 } or range('a'..'f').map { it - 'a' + 10 }
    private val escaped = -char('\\') +
            oneOf(
                    char('b').map { '\u0008' },
                    char('f').map { '\u000C' },
                    char('n').map { '\n' },
                    char('r').map { '\r' },
                    char('t').map { '\t' },
                    char('\'').map { '\'' },
                    char('\"').map { '\"' },
                    char('\\').map { '\\' },
                    -char('x') + (HEX_DIGIT + HEX_DIGIT).map { (a, b) -> (b + 16 * a).toChar() },
                    -char('u') + (HEX_DIGIT + HEX_DIGIT + HEX_DIGIT + HEX_DIGIT).map { (a,b,c,d) ->
                        (d + 16 * (c + 16 * (b + 16 * a))).toChar()
                    },
                    (OCT_DIGIT + OCT_DIGIT.orNot() + OCT_DIGIT.orNot()).map {
                        (a, b, c) ->
                        var result = a!!
                        b?.let { result *= 16; result += b }
                        c?.let { result *= 16; result += c }
                        result.toChar()
                    }
            )

    val CSTRING: Parser<Char, String> =
            -char('"') +
                    ( char{ it != '\"' && it != '\\' } or escaped ).many().map { it.joinToString("") } +
                    -char('"') named "C String"
}
