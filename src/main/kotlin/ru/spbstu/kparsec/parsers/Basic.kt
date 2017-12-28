package ru.spbstu.kparsec.parsers

import kotlinx.Warnings
import ru.spbstu.kparsec.*

@Suppress(Warnings.NOTHING_TO_INLINE)
internal inline fun<T, R> Parser<T, R>.asParser() = this

data class SuccessParser<T, R>(val result: R): Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> = Success(input, result)
}

fun<T, R> success(result: R) = SuccessParser<T, R>(result).asParser()

data class ErrorParser<T>(val error: String): Parser<T, Nothing> {
    override fun invoke(input: Input<T>): ParseResult<T, Nothing> = Failure(error, input.location)
}

fun<T> fail(error: String) = ErrorParser<T>(error).asParser()

data class NamedParser<T, R>(val name: String, val inner: Parser<T, R>): Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> {
        val parse = inner(input)
        return when(parse) {
            is Success -> parse
            is Failure -> parse.copy(expected = name)
        }
    }
}

infix fun<T, R> Parser<T, R>.named(name: String) = NamedParser(name, this).asParser()

data class ConstantParser(val c: String): Parser<Char, String> {
    override fun invoke(input: Input<Char>): ParseResult<Char, String> {
        return when {
            input.source.asCharSequence().startsWith(c) -> Success(input.drop(c.length), c)
            else -> Failure("\"$c\"", input.location)
        }
    }
}

fun constant(c: String) = ConstantParser(c).asParser()

data class RegexParser(val r: Regex): Parser<Char, String> {
    override fun invoke(input: Input<Char>): ParseResult<Char, String> {
        val mtcher = r.toPattern().matcher(input.source.asCharSequence())
        return when {
            mtcher.lookingAt() -> Success(input.drop(mtcher.end()), mtcher.group())
            else -> Failure("regex $r", input.location)
        }
    }
}

fun regex(str: String) = RegexParser(str.toRegex()).asParser()
fun regex(re: Regex) = RegexParser(re).asParser()

data class TokenParser<T>(val testDescription: String, val test: (T) -> Boolean): Parser<T, T> {
    override fun invoke(input: Input<T>) = run {
        val first = input.source.firstOrNull()
        when {
            first != null && test(first) -> Success(input.next(), first)
            else -> Failure(testDescription, input.location)
        }
    }
}

fun char(ch: Char) = TokenParser<Char>("'$ch'") { it == ch }.asParser()
fun char(predicate: (Char) -> Boolean) = TokenParser("#predicate#", predicate).asParser()
fun anyChar() = TokenParser<Char>("any character"){ true }.asParser()
fun range(range: CharRange) = TokenParser<Char>("$range"){ it in range }.asParser()
fun oneOf(ch: String) = TokenParser<Char>("oneOf(\"${ch}\")") { it in ch }.asParser()

fun<T> token(ch: T) = TokenParser<T>("'$ch'") { it == ch }.asParser()
fun<T> token(predicate: (T) -> Boolean) = TokenParser("#predicate#", predicate).asParser()
fun<T> anyToken(): Parser<T, T> = TokenParser<T>("any token"){ true }.asParser()
fun<T: Comparable<T>> range(range: ClosedRange<T>) = TokenParser<T>("$range"){ it in range }.asParser()
fun<T> oneOf(ch: Collection<T>) = TokenParser<T>("oneOf(${ch.joinToString()})") { it in ch }.asParser()
fun<T> oneOf(vararg ch: T) = oneOf(ch.asList()).asParser()

class EofParser<T>: Parser<T, Unit> {
    override fun invoke(input: Input<T>) = when {
        input.source.isEmpty() -> Success(input, Unit)
        else -> Failure("<EOF>", input.location)
    }
}

fun <T> eof() = EofParser<T>().asParser()
