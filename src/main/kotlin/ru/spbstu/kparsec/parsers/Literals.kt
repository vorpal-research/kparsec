package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*

/**
 * Parsers for some commonly used literals
 */
object Literals {
    /**
     * Any number of whitespace characters
     */
    val SPACES: Parser<Char, String> = regex("\\s*".toRegex()).asParser()

    /**
     * A parser [inner] surrounded by any number of spaces
     */
    fun<T> lexeme(inner: Parser<Char, T>): Parser<Char, T> = -SPACES + inner + -SPACES
    /**
     * String [inner] surrounded by any number of spaces
     */
    fun lexeme(inner: String): Parser<Char, String> = lexeme(constant(inner))
    /**
     * Char [inner] surrounded by any number of spaces
     */
    fun lexeme(inner: Char): Parser<Char, Char> = lexeme(char(inner))

    /**
     * Standard float literal
     */
    val FLOAT: Parser<Char, Double> =
            regex("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?".toRegex()).map { it.toDouble() } named "Float"
    /**
     * Standard decimal number literal
     */
    val DECIMAL: Parser<Char, Long> =
            regex("[0-9]+".toRegex()).map { it.toLong() } named "Decimal"
    /**
     * Standard octal number literal
     */
    val OCTAL: Parser<Char, Long> =
            regex("[0-7]+".toRegex()).map { it.toLong(8) } named "Octal"
    /**
     * Standard hexadecimal number literal
     */
    val HEXADECIMAL: Parser<Char, Long> =
            regex("[0-9a-fA-F]+".toRegex()).map { it.toLong(16) } named "Hexadecimal"
    /**
     * Standard [Boolean] literal
     */
    val BOOLEAN: Parser<Char, Boolean> =
            constant("true").map { true } or constant("false").map { false } named "Boolean"


    private val CDECIMAL: Parser<Char, Long> =
            regex("[1-9][0-9]*".toRegex()).map { it.toLong() } named "Decimal"
    private val COCTAL: Parser<Char, Long> =
            regex("0[0-7]*".toRegex()).map { it.toLong(8) } named "Octal"
    private val CHEXADECIMAL: Parser<Char, Long> =
            regex("0[xX][0-9a-fA-F]+".toRegex()).map { it.drop(2).toLong(16) } named "Hexadecimal"
    /**
     * C-like number literal, supporting 0- and 0x- prefixed numbers
     */
    val CINTEGER: Parser<Char, Long> = CHEXADECIMAL or COCTAL or CDECIMAL

    val OCT_DIGIT: Parser<Char, Int> = range('0'..'7').map { it - '0' }
    val DEC_DIGIT: Parser<Char, Int> = range('0'..'9').map { it - '0' }
    val HEX_DIGIT: Parser<Char, Int> =
            DEC_DIGIT or range('A'..'F').map { it - 'A' + 10 } or range('a'..'f').map { it - 'a' + 10 }
    private val escaped: Parser<Char, Char> = -char('\\') +
            oneOf(
                    char('b').map { '\u0008' },
                    char('f').map { '\u000C' },
                    char('n').map { '\n' },
                    char('r').map { '\r' },
                    char('t').map { '\t' },
                    char('\'').map { '\'' },
                    char('\"').map { '\"' },
                    char('\\').map { '\\' },
                    -char('x') + (HEX_DIGIT * 2).map { (a, b) -> (b + 16 * a).toChar() },
                    -char('u') + (HEX_DIGIT * 4).map { (a,b,c,d) ->
                        (d + 16 * (c + 16 * (b + 16 * a))).toChar()
                    },
                    (OCT_DIGIT * (1..3)).map { lst ->
                        val a = lst.get(0)
                        val b = lst.getOrNull(1)
                        val c = lst.getOrNull(2)
                        var result = a
                        b?.let { result *= 8; result += b }
                        c?.let { result *= 8; result += c }
                        result.toChar()
                    }
            )

    /**
     * JSON-like string literal, surrounded by double quotes and supporting character escapes
     * (including \\x and \\u escaping)
     */
    val JSTRING: Parser<Char, String> =
            -char('"') +
                    ( char{ it != '\"' && it != '\\' } or escaped ).many().map { it.joinToString("") } +
                    -char('"') named "J String"
}
