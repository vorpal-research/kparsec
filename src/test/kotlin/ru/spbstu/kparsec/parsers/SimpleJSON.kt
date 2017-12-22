package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*
import ru.spbstu.kparsec.Literals.lexeme

object SimpleJSONParser {
    val string = Literals.CSTRING
    val number = Literals.FLOAT
    val boolean = Literals.BOOLEAN
    val nully = constant("null").map { null }

    val arrayElements = defer { element } joinedBy -lexeme(',') orElse emptyList()
    val array = -lexeme('[') + arrayElements + -lexeme(']')

    val entry_ = string + -lexeme(':') + defer { element }
    val entry = entry_.map { (a, b) -> a to b }

    val objectElements = entry joinedBy -lexeme(',') orElse emptyList()
    val obj = -lexeme('{') + objectElements + -lexeme('}')

    val element: Parser<Char, Any?> = nully or string or number or boolean or array or obj
    val whole = element + eof()
}
