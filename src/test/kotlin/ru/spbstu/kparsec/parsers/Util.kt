package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Failure
import ru.spbstu.kparsec.ParseResult
import ru.spbstu.kparsec.Success

fun<T, R> ParseResult<T, R>.assertResult(): R {
    require(this is Success)
    return (this as Success).result
}

fun<T, R> ParseResult<T, R>.assertFail() {
    require(this is Failure)
}
