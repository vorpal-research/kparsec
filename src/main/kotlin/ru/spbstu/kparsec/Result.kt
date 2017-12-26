package ru.spbstu.kparsec

import kotlinx.Warnings

sealed class ParseResult<out T, out R>
data class Success<out T, out R>(val rest: Input<T>, val result: R): ParseResult<T, R>()
data class Failure(val expected: String, val location: Location): ParseResult<Nothing, Nothing>()

@Suppress(Warnings.NOTHING_TO_INLINE)
inline fun<T, R, S> ParseResult<T, R>.map(f: (R) -> S): ParseResult<T, S> = when(this) {
    is Success -> Success(rest, result = f(result))
    is Failure -> this
}
