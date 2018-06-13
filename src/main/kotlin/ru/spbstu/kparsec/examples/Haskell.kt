package ru.spbstu.kparsec.examples

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*

object HaskellLexer : StringsAsParsers {

    enum class TokenType {
        WS, COMMENT, KEYWORD, IDEN
    }

    val program by lazy {
        many(lexema)
    }
    val lexema by lazy {
        qvarid or qconid or qvarsym or qconsym or literal or special or reservedop or reservedid
    }
    val literal by lazy { integer or float or char or string }

    val special = -oneOf("(),;[]`{}")
    val whitespace by lazy {
        whitestuff.manyOne().map { TokenType.WS }
    }
    val whitestuff by lazy {
        whitechar or comment or ncomment
    }
    val whitechar by lazy {
        newline or -char { it.isWhitespace() }
    }
    val newline by lazy {
        -constant("\r\n") or -char('\r') or -char('\n') or -char('\u000c')
    }

    val comment by lazy {
        dashes + anyChar().many().map { TokenType.COMMENT } + newline
    }
    val dashes by lazy {
        -constant("--") + -char('-').many()
    }
    val opencom = -constant("{-")
    val closecom = -constant("-}")

    val ncomment: Parser<Char, Any> by lazy {
        opencom + ANYseq + (defer { ncomment } + ANYseq).many() + closecom
    }
    val ANYseq by lazy {
        ANY.many() excluding  (opencom + ANY.many() + closecom)
    }
    val ANY by lazy {
        graphic or whitechar
    }
    val graphic: Parser<Char, Any> by lazy {
        small or large or symbol or digit or special or char(':') or char('"') or char('\'')
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

    val symbol by lazy {
        ascSymbol or (uniSymbol excluding special)
    }
    val ascSymbol = oneOf("!#$%&*+./<=>?@")
    val uniSymbol = char { false /* TODO */}

    val digit by lazy {
        ascDigit or uniDigit
    }
    val ascDigit = range('0'..'9')
    val uniDigit = char { it.isDigit() }
    val octit = range('0'..'7')
    val hexit = digit or range('A'..'F') or range('a'..'f')
    val varid by lazy {
        (small + (small or large or digit or char('\'')).many()) excluding reservedid
    }
    val conid by lazy {
        large + (small or large or digit or char('\'')).many()
    }
    val reservedid = oneOf(
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

    val varsym by lazy {
        symbol + many(symbol or char(':')).excluding (reservedop, dashes)
    }
    val consym by lazy {
        char(':') + many(symbol or char(':')) excluding reservedop
    }

    val reservedop = oneOf(
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

    val tyvar = varid
    val tycon = conid
    val tycls = conid
    val modid = conid
    val qvarid = maybe(modid + char('.')) + varid
    val qconid = maybe(modid + char('.')) + conid
    val qtycon = maybe(modid + char('.')) + tycon
    val qtycls = maybe(modid + char('.')) + tycls
    val qvarsym = maybe(modid + char('.')) + varsym
    val qconsym = maybe(modid + char('.')) + consym

    val decimal = manyOne(digit)
    val octal = manyOne(octit)
    val hexadecimal = manyOne(hexit)

    val integer by lazy {
        decimal or (constant("0o") + octal) or (constant("0O") + octal) or
                (constant("0x") + hexadecimal) or (constant("0X") + hexadecimal)
    }
    val float by lazy {
        (decimal + char('.') + decimal + maybe(exponent)) or
                (decimal + exponent)
    }
    val exponent by lazy {
        oneOf("eE") + maybe(oneOf("+-")) + decimal
    }
    val char by lazy {
        char('\'') + (
                (graphic excluding oneOf("'\\")) or char(' ') or (escape excluding constant("\\&"))
                ) + char('\'')
    }
    val string by lazy {
        char('"') + (
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

    override val skippedBefore: Parser<Char, Any?> = whitespace

}