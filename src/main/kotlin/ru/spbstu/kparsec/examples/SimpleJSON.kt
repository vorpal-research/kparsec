package ru.spbstu.kparsec.examples

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*

object SimpleJSONParser: StringsAsParsers, DelegateParser<Char, Any?> {
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

    override val self = element + eof()
}
