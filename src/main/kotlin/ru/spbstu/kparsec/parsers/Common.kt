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

}
