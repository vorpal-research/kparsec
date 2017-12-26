package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Parser

object SimpleJSONParser: StringsAsParsers {
    val string = Literals.JSTRING
    val number = Literals.FLOAT
    val boolean = Literals.BOOLEAN
    val nully = (+"null").map { null }

    val arrayElements = defer { element } joinedBy -',' orElse emptyList()
    val array = -'[' + arrayElements + -']'

    val entry_ = string + -':' + defer { element }
    val entry = entry_.map { (a, b) -> a to b }

    val objectElements = entry joinedBy -',' orElse emptyList()
    val obj = -'{' + objectElements + -'}'

    val element: Parser<Char, Any?> = nully or string or number or boolean or array or obj
    val whole = element + eof()
}
