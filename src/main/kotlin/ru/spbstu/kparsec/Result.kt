package ru.spbstu.kparsec

import kotlinx.Warnings

/**
 * The result of parsing
 * @param T the type of token (*Char* for strings)
 * @param R the actual result type
 * @see Success
 * @see Failure
 */
sealed class ParseResult<out T, out R>

/**
 * Successful parse result
 * @param T the type of token (*Char* for strings)
 * @param R the actual result type
 * @property rest the rest of input
 * @property result the actual result
 */
data class Success<out T, out R>(val rest: Input<T>, val result: R): ParseResult<T, R>()

/**
 * Parse result representing failure
 * @property expected string representation of what was expected here
 * @property location current source location
 */
data class Failure(val expected: String, val location: Location): ParseResult<Nothing, Nothing>()

/**
 * Transform the result value using a function [f]
 * @return [Success] with current result if was a [Success]
 * @return [this] if result was a [Failure]
 */
@Suppress(Warnings.NOTHING_TO_INLINE)
inline fun<T, R, S> ParseResult<T, R>.map(f: (R) -> S): ParseResult<T, S> = when(this) {
    is Success -> Success(rest, result = f(result))
    is Failure -> this
}
