package ru.spbstu.kparsec

interface Parser<T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<T, R>
}

fun<T> Parser<Char, T>.parse(string: String): ParseResult<Char, T> = this(StringInput(string))
fun<T, E> Parser<T, E>.parse(data: List<T>): ParseResult<T, E> = this(ListInput(data))
fun<T, E> Parser<T, E>.parse(data: Array<T>): ParseResult<T, E> = this(ListInput(data.asList()))
