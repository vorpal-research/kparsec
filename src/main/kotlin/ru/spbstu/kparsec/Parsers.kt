package ru.spbstu.kparsec

interface Parser<T, out R> {
    operator fun invoke(input: Input<T>): ParseResult<T, R>
}

fun<T> Parser<Char, T>.parse(string: String) = this(StringInput(string))
