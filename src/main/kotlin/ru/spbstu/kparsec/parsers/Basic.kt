package ru.spbstu.kparsec.parsers

import kotlinx.Warnings
import ru.spbstu.kparsec.*
import ru.spbstu.kparsec.wheels.escape

/**
 * Internal stuff
 */
@Suppress(Warnings.NOTHING_TO_INLINE)
internal inline fun<T, R> Parser<T, R>.asParser() = this

/**
 * [Parser] that parses nothing, always succeeds and returns [result]
 * @see success
 */
data class SuccessParser<T, R>(val result: R): Parser<T, R> {
    override fun invoke(input: Source<T>): ParseResult<T, R> = Success(input, result)
    override val description: String
        get() = "success($result)"
}

/**
 * [Parser] that parses nothing, always succeeds and returns nothing
 * @see SuccessParser
 */
fun<T> empty(): Parser<T, Unit> = SuccessParser<T, Unit>(Unit) named "<>"
/**
 * [Parser] that parses nothing, always succeeds and returns [result]
 * @see SuccessParser
 */
fun<T, R> success(result: R): Parser<T, R> = SuccessParser<T, R>(result).asParser()

/**
 * [Parser] that parses nothing and always fails with [error]
 * @see fail
 */
data class ErrorParser<T>(val error: String): Parser<T, Nothing> {
    override fun invoke(input: Source<T>): ParseResult<T, Nothing> = Failure(error, input.location)
    override val description: String
        get() = "error($error)"
}

/**
 * [Parser] that parses nothing and always fails with [error]
 * @see ErrorParser
 */
fun<T> fail(error: String): Parser<T, Nothing> = ErrorParser<T>(error).asParser()

/**
 * [Parser] that wraps another parser [inner], but has a different [name]
 * @see named
 */
data class NamedParser<T, R>(val name: String, val inner: Parser<T, R>): Parser<T, R> {
    override fun invoke(input: Source<T>): ParseResult<T, R> {
        val parse = inner(input)
        return when(parse) {
            is Success -> parse
            is NoSuccess -> parse /*.copy(expected = name)*/
        }
    }

    override val description: String
        get() = name
}

/**
 * [Parser] that wraps another parser [inner], but has a different [name]
 * @see NamedParser
 */
infix fun<T, R> Parser<T, R>.named(name: String): Parser<T, R> = NamedParser(name, this).asParser()

/**
 * [Parser] that only parses a constant string [c]
 * @see constant
 */
data class ConstantParser(val c: String): Parser<Char, String> {
    override fun invoke(input: Source<Char>): ParseResult<Char, String> {
        return when {
            input.asCharSequence().startsWith(c) -> Success(input.drop(c.length), c)
            else -> Failure("\"${c.escape()}\"", input.location)
        }
    }

    override val description: String
        get() = "\"${c.escape()}\""
}

/**
 * [Parser] that only parses a constant string [c]
 * @see ConstantParser
 */
fun constant(c: String): Parser<Char, String> = ConstantParser(c).asParser()

/**
 * [Parser] that works the same way as the regular expression [r]
 * @see regex
 */
data class RegexParser(val r: Regex): Parser<Char, String> {
    override fun invoke(input: Source<Char>): ParseResult<Char, String> {
        val mtcher = r.toPattern().matcher(input.asCharSequence())
        return when {
            mtcher.lookingAt() -> Success(input.drop(mtcher.end()), mtcher.group())
            else -> Failure("regex $r", input.location)
        }
    }

    override val description: String
        get() = "/${r.pattern}/"
}

/**
 * [Parser] that works the same way as the regular expression [str]
 * @see RegexParser
 */
fun regex(str: String): Parser<Char, String> = RegexParser(str.toRegex()).asParser()
/**
 * [Parser] that works the same way as the regular expression [re]
 * @see RegexParser
 */
fun regex(re: Regex): Parser<Char, String> = RegexParser(re).asParser()

/**
 * [Parser] that parses a single token iff it corresponds to [test]
 * @property testDescription the name of the parse
 * @see token
 * @see char
 */
data class TokenParser<T>(val testDescription: String, val test: (T) -> Boolean): Parser<T, T> {
    override fun invoke(input: Source<T>) = run {
        val first = input.currentOrNull()
        when {
            first != null && test(first) -> Success(input.next(), first)
            else -> Failure(testDescription, input.location)
        }
    }

    override val description: String
        get() = testDescription
}

/**
 * [Parser] that parses a single char iff it equals to [ch]
 */
fun char(ch: Char): Parser<Char, Char> = TokenParser<Char>("'$ch'") { it == ch }.asParser()
/**
 * [Parser] that parses a single token iff it corresponds to [predicate]
 */
fun char(predicate: (Char) -> Boolean): Parser<Char, Char> =
        TokenParser("#predicate#", predicate).asParser()
/**
 * [Parser] that parses any character
 */
fun anyChar(): Parser<Char, Char> = TokenParser<Char>("any character"){ true }.asParser()
/**
 * [Parser] that parses a single character if it is in [range]
 */
fun range(range: CharRange): Parser<Char, Char> = TokenParser<Char>("$range"){ it in range }.asParser()
/**
 * [Parser] that parses a single character if it is contained in string [ch]
 */
fun oneOf(ch: String): Parser<Char, Char> = TokenParser<Char>("oneOf(\"${ch}\")") { it in ch }.asParser()

/**
 * [Parser] that parses a single token iff it equals to [ch]
 */
fun<T> token(ch: T): Parser<T, T> = TokenParser<T>("'$ch'") { it == ch }.asParser()
/**
 * [Parser] that parses a single token iff it corresponds to [predicate]
 */
fun<T> token(predicate: (T) -> Boolean): Parser<T, T> = TokenParser("#predicate#", predicate).asParser()
/**
 * [Parser] that parses any single token
 */
fun<T> anyToken(): Parser<T, T> = TokenParser<T>("any token"){ true }.asParser()
/**
 * [Parser] that parses any token iff it is in [range]
 */
fun<T: Comparable<T>> range(range: ClosedRange<T>): Parser<T, T> =
        TokenParser<T>("$range"){ it in range }.asParser()
/**
 * [Parser] that parses any token iff it is in [ch]
 */
fun<T> oneOf(ch: Collection<T>): Parser<T, T> =
        TokenParser<T>("oneOf(${ch.joinToString()})") { it in ch }.asParser()
/**
 * [Parser] that parses any token iff it is in [ch]
 */
fun<T> oneOf(vararg ch: T) = oneOf(ch.asList()).asParser()

/**
 * The end of input. Succeeds if there is no input left.
 */
class EofParser<T>: Parser<T, Unit> {
    override fun invoke(input: Source<T>) = when {
        input.isEmpty() -> Success(input, Unit)
        else -> Failure("<EOF>", input.location)
    }

    override val description: String
        get() = "<EOF>"
}

/**
 * The end of input. Succeeds if there is no input left.
 */
fun <T> eof(): Parser<T, Unit> = EofParser<T>().asParser()
