package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*

/**
 * Parses input using [base], but does not consume anything.
 */
data class LookaheadParser<T, A>(val base: Parser<T, A>): Parser<T, A> {
    override fun invoke(input: Source<T>): ParseResult<T, A> {
        val first = base(input)
        return when(first) {
            is NoSuccess -> first
            is Success -> first.copy(rest = input)
        }
    }

    override val description: String
        get() = base.description
}

/**
 * Parses input using [base], but does not consume anything.
 * Be cautious when using this!
 */
fun <T, A> lookahead(base: Parser<T, A>): Parser<T, A> = LookaheadParser(base)
infix fun <T, A, B> Parser<T, A>.followedBy(other: Parser<T, B>) = this + -lookahead(other)

/**
 * Parses input using [base], but reverses the results (fails on success and vice versa)
 * Never consumes any input
 */
data class NotParser<T>(val base: Parser<T, Any?>): Parser<T, Unit> {
    override fun invoke(input: Source<T>): ParseResult<T, Unit> {
        val first = base(input)
        return when(first) {
            is Failure -> Success(input, Unit)
            is Success -> Failure("not($base)", input.location)
            is Error -> first
        }
    }

    override val description: String
        get() = "not(${base.description})"
}
/**
 * Parses input using [base], but does not consume anything and reverses the results.
 * Be cautious when using this!
 */
fun <T, A> not(base: Parser<T, A>): Parser<T, Unit> = NotParser(base)
infix fun <T, A, B> Parser<T, A>.notFollowedBy(other: Parser<T, B>) = this + not(other)
