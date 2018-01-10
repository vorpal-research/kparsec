package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Parser

object Common : StringsAsParsers {

    val IDENTIFIER: Parser<Char, String> = regex(Regex("""[a-zA-Z_][a-zA-Z_0-9]*"""))
    val JAVA_IDENTIFIER: Parser<Char, String> = regex(Regex("""[a-zA-Z_$][a-zA-Z_$0-9]*"""))
    val KOTLIN_IDENTIFIER: Parser<Char, String> = regex(Regex("""`[^`]*`""")) or JAVA_IDENTIFIER

}