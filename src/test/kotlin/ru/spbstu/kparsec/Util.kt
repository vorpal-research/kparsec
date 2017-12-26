package ru.spbstu.kparsec

fun<T, R> ParseResult<T, R>.assertResult(): R {
    require(this is Success)
    return (this as Success).result
}

fun<T, R> ParseResult<T, R>.assertFail() {
    require(this is Failure)
}
