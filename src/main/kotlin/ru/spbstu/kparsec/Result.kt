package ru.spbstu.kparsec

import kotlinx.warnings.Warnings

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
data class Success<out T, out R>(val rest: Source<T>, val result: R): ParseResult<T, R>()

sealed class NoSuccess: ParseResult<Nothing, Nothing>() {
    abstract val expected: String
    abstract val location: Location<*>
    abstract fun copy(expected: String = this.expected, location: Location<*> = this.location): NoSuccess
}

/**
 * Parse result representing failure
 * @property expected string representation of what was expected here
 * @property location current source location
 */
@Suppress("DATA_CLASS_OVERRIDE")
class Failure(override val expected: String, override val location: Location<*>): NoSuccess() {
    override fun copy(expected: String, location: Location<*>): Failure =
        Failure(expected, location)

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is Failure -> false
        expected != other.expected -> false
        location != other.location -> false
        else -> true
    }

    override fun hashCode(): Int {
        var result = expected.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }

    override fun toString(): String {
        return "Failure(expected='$expected', location=$location)"
    }
}
@Suppress("DATA_CLASS_OVERRIDE_DEFAULT_VALUES")
class Error(override val expected: String, override val location: Location<*>): NoSuccess() {
    override fun copy(expected: String, location: Location<*>): Error =
        Error(expected, location)

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is Error -> false
        expected != other.expected -> false
        location != other.location -> false
        else -> true
    }

    override fun hashCode(): Int {
        var result = expected.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }

    override fun toString(): String {
        return "Error(expected='$expected', location=$location)"
    }
}

/**
 * Transform the result value using a function [f]
 * @return [Success] with current result if was a [Success]
 * @return [this] if result was a [Failure]
 */
@Suppress(Warnings.NOTHING_TO_INLINE)
inline fun<T, R, S> ParseResult<T, R>.map(f: (R) -> S): ParseResult<T, S> = when(this) {
    is Success -> Success(rest, result = f(result))
    is NoSuccess -> this
}
