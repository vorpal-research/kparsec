package ru.spbstu.kparsec

sealed class ParseResult<out T, out R>
data class Success<out T, out R>(val rest: Input<T>, val result: R): ParseResult<T, R>()
data class Failure(val expected: String, val location: Location): ParseResult<Nothing, Nothing>()
