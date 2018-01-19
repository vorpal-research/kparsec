package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Parser

/**
 * A set of commonly used parsers
 * @see Literals
 */
object Common : StringsAsParsers {
    /**
     * Standard identifier `[a-zA-Z_][a-zA-Z_0-9]*`
     */
    val IDENTIFIER: Parser<Char, String> = regex(Regex("""[a-zA-Z_][a-zA-Z_0-9]*"""))
    /**
     * Java identifier (allowing dollars)
     */
    val JAVA_IDENTIFIER: Parser<Char, String> = regex(Regex("""[a-zA-Z_$][a-zA-Z_$0-9]*"""))
    /**
     * Kotlin identifier (same as Java identifier, but supporting backtick-escaping)
     */
    val KOTLIN_IDENTIFIER: Parser<Char, String> = regex(Regex("""`[^`]*`""")) or JAVA_IDENTIFIER
    /**
     * Common newline terminator: CR or LF or CRLF
     */
    val NEWLINE: Parser<Char, Unit> = (-char('\r') + -char('\n').orNot()) or -char('\n')

    fun lineComment(begin: Parser<Char, Any?>) = -begin + regex(Regex("""[^\r\n]*"""))
    val C_LINE_COMMENT: Parser<Char, String> = lineComment(constant("//"))
    val BASH_LINE_COMMENT: Parser<Char, String> = lineComment(constant("#"))

}
