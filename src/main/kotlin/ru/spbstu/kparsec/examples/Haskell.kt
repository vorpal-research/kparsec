package ru.spbstu.kparsec.examples.haskell

import ru.spbstu.kparsec.CharLocation
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*
import kotlin.reflect.KProperty

data class Token(val type: String, val value: Any?, val location: Pair<CharLocation, CharLocation>) {
    override fun toString(): String = when(value) {
        is Unit -> type
        is Collection<*> -> "$type(${value.joinToString()})"
        else -> "$type($value)"
    }
}

object HaskellLexer : StringsAsParsers {
    internal fun Parser<Char, String>.asTokenParser(type: String) =
            withLocation  { before, result, after ->
                before as CharLocation
                after as CharLocation
                Token(type, result.replace(" ", "\\w".replace("\n", "\\n".replace("\t", "\\t"))), before to after)
            }

    object LazyTokenTombstone
    private inline fun <T> lazyToken(noinline body: () -> Parser<Char, T>) = object {
        var lazy: Any? = LazyTokenTombstone
        operator fun getValue(thisRef: Any?, prop: KProperty<*>): Parser<Char, Token> {
            when(lazy) {
                LazyTokenTombstone -> {
                    return string(body()).asTokenParser(prop.name).also { lazy = it }
                }
                else ->
                    @Suppress("UNCHECKED_CAST")
                    return lazy as Parser<Char, Token>
            }
        }
    }
    private fun string(inp: Parser<Char, Any?>): Parser<Char, String> {
        fun mapper(it: Any?): String = when(it) {
            is Token -> mapper(it.value)
            is String -> it
            is Collection<*> -> it.joinToString("", transform = ::mapper)
            is Unit -> ""
            else -> "$it"
        }
        return inp.map(::mapper)
    }

    val program: Parser<Char, List<Token>> by lazy {
        spaces + many(lexema + spaces).map { it.flatten() }
    }
    val lexema by lazy {
        reservedop or reservedid or special or qvarid or qconid or qvarsym or qconsym or literal
    }
    val literal by lazy { integer or float or charLiteral or stringLiteral }

    val specialChar = oneOf("(),;[]`{}")
    val special by lazyToken { specialChar }
    val whitespace by lazy {
        whitestuff.manyOne()
    }
    val spaces by lazyToken { many(whitespace) }
    val whitestuff by lazy {
        whitechar or comment or ncomment
    }
    val whitechar by lazy {
        newline or -char { it.isWhitespace() }
    }
    val newline by lazy {
        constant("\r\n") or char('\r') or char('\n') or char('\u000c')
    }

    val comment by lazyToken {
        dashes + (anyChar() excluding newline).many() + newline
    }
    val dashes by lazy {
        -constant("--") + -char('-').many()
    }
    val opencom = -constant("{-")
    val closecom = -constant("-}")

    val ncomment: Parser<Char, Token> by lazyToken {
        opencom + (defer { ncomment } or (ANY excluding closecom)).many() + closecom
    }
    val ANYseq by lazy {
        ANY.many() excluding  (opencom + ANY.many() + closecom)
    }
    val ANY by lazy {
        graphic or whitechar
    }
    val graphic: Parser<Char, Any> by lazy {
        small or large or symbol or digit or specialChar or char(':') or char('"') or char('\'')
    }
    val small by lazy {
        ascSmall or uniSmall or char('_')
    }
    val ascSmall = range('a'..'z')
    val uniSmall = char { it.isLowerCase() }
    val large by lazy {
        ascLarge or uniLarge
    }
    val ascLarge = range('A'..'Z')
    val uniLarge = char { it.isUpperCase() }

    val symbol by lazyToken {
        ascSymbol or (uniSymbol excluding specialChar)
    }
    val ascSymbol = oneOf("!#$%&|*+-./<=>?@")
    val uniSymbol = char { false /* TODO */}

    val digit: Parser<Char, Char> by lazy {
        ascDigit or uniDigit
    }
    val ascDigit = range('0'..'9')
    val uniDigit = char { it.isDigit() }
    val octit = range('0'..'7')
    val hexit = digit or range('A'..'F') or range('a'..'f')
    val varid by lazyToken { string(
        (small + (small or large or digit or char('\'')).many()) excluding reservedid
    )}
    val conid by lazyToken { string(
        large + (small or large or digit or char('\'')).many()
    )}
    val reservedid by lazyToken {
        oneOf(
                constant("case"),
                constant("class"),
                constant("data"),
                constant("default"),
                constant("deriving"),
                constant("do"),
                constant("else"),
                constant("if"),
                constant("import"),
                constant("in"),
                constant("infixl"),
                constant("infixr"),
                constant("infix"),
                constant("instance"),
                constant("let"),
                constant("module"),
                constant("newtype"),
                constant("of"),
                constant("then"),
                constant("type"),
                constant("where"),
                constant("_")
        )
    }

    val varsym by lazyToken {
        (symbol + many(symbol or char(':'))).excluding (reservedop, dashes)
    }
    val consym by lazyToken {
        char(':') + many(symbol or char(':')) excluding reservedop
    }

    val reservedop by lazyToken {
        oneOf(
                constant(".."),
                constant("::"),
                constant(":"),
                constant("="),
                constant("\\"),
                constant("|"),
                constant("<-"),
                constant("->"),
                constant("@"),
                constant("~"),
                constant("=>")
        )
    }

    val tyvar by lazyToken { varid }
    val tycon by lazyToken { conid }
    val tycls by lazyToken { conid }
    val modid by lazyToken { conid }
    val qvarid  by lazyToken { many(modid + char('.')) + varid }
    val qconid  by lazyToken { many(modid + char('.')) + conid }
    val qtycon  by lazyToken { many(modid + char('.')) + tycon }
    val qtycls  by lazyToken { many(modid + char('.')) + tycls }
    val qvarsym by lazyToken { many(modid + char('.')) + varsym }
    val qconsym by lazyToken { many(modid + char('.')) + consym }

    val decimal = manyOne(digit).map { it.joinToString("") }
    val octal = manyOne(octit).map { it.joinToString("") }
    val hexadecimal = manyOne(hexit).map { it.joinToString("") }

    val integer by lazyToken {
        decimal or (constant("0o") + octal) or (constant("0O") + octal) or
                (constant("0x") + hexadecimal) or (constant("0X") + hexadecimal)
    }
    val float by lazyToken {
        (decimal + char('.') + decimal + maybe(exponent)) or
                (decimal + exponent)
    }
    val exponent by lazy {
        oneOf("eE") + maybe(oneOf("+-")) + decimal
    }
    val charLiteral by lazyToken {
        char('\'') + (
                (graphic excluding oneOf("'\\")) or char(' ') or (escape excluding constant("\\&"))
                ) + char('\'')
    }
    val stringLiteral by lazyToken {
        char('"') + many(
                (graphic excluding oneOf("\"\\")) or char(' ') or escape or gap
                ) + char('"')
    }
    val escape by lazy {
        char('\\') + (charesc or ascii or decimal or (char('o') + octal) or (char('x') + hexadecimal))
    }
    val charesc = oneOf("abfnrtv\\\"\'&")
    val ascii by lazy { cntrl }
    val cntrl by lazy {
        ascLarge or oneOf("@[\\]^_")
    }
    val gap by lazy {
        char('\\') + manyOne(whitechar) + char('\\')
    }
}