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
fun<T: Comparable<T>> range(range: ClosedRange<T>) = TokenParser<T>("$range"){ it in range }.asParser()
fun<T> oneOf(ch: Collection<T>) = TokenParser<T>("oneOf(${ch.joinToString()})") { it in ch }
fun<T> oneOf(vararg ch: T) = oneOf(ch.asList())

data class ZipParser<T, A, B, R>(val lhv: Parser<T, A>, val rhv: Parser<T, B>, val f: (A, B) -> R): Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> {
        val lr = lhv(input)
        return when(lr) {
            is Success -> {
                val rr = rhv(lr.rest)
                when(rr) {
                    is Success -> Success(rr.rest, f(lr.result, rr.result))
                    is Failure -> rr
                }
            }
            is Failure -> lr
        }
    }
}

fun <T, A, B, R> zip(lhv: Parser<T, A>, rhv: Parser<T, B>, f: (A, B) -> R) = ZipParser(lhv, rhv, f).asParser()

data class SeqParser<T, A>(val elements: Iterable<Parser<T, A>>): Parser<T, Collection<A>> {
    constructor(vararg elements: Parser<T, A>): this(elements.asList())

    override fun invoke(input: Input<T>): ParseResult<T, Collection<A>> {
        val it = elements.iterator()
        if(!it.hasNext()) return Success(input, listOf())

        val result = mutableListOf<A>()

        var currentResult = it.next().invoke(input)
        for(parser in it) {
            when(currentResult) {
                is Failure -> return currentResult
                is Success -> {
                    result += currentResult.result
                    currentResult = parser(currentResult.rest)
                }
            }
        }
        return when(currentResult) {
            is Success -> Success(currentResult.rest, result)
            is Failure -> currentResult
        }
    }
}

fun <T, A> sequence(vararg parsers: Parser<T, A>) = SeqParser(*parsers).asParser()

data class ChoiceParser<T, A>(val elements: Iterable<Parser<T, A>>): Parser<T, A> {
    constructor(vararg elements: Parser<T, A>): this(elements.asList())

    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val it = elements.iterator()
        if(!it.hasNext()) return Failure("<empty choice>", input.location)

        var currentResult = it.next().invoke(input)
        for(parser in it) {
            when(currentResult) {
                is Success -> return currentResult
                is Failure -> {
                    currentResult = parser(input)
                }
            }
        }
        return currentResult
    }
}

fun <T, A> oneOf(vararg parsers: Parser<T, A>) = ChoiceParser(*parsers).asParser()

data class MapParser<T, A, R>(val lhv: Parser<T, A>, val f: (A) -> R): Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> {
        val r = lhv(input)
        return when(r) {
            is Success -> Success(r.rest, f(r.result))
            is Failure -> r
        }
    }
}

fun <T, A, R> Parser<T, A>.map(f: (A) -> R) = MapParser(this, f).asParser()

data class OrParser<T, A>(val lhv: Parser<T, A>, val rhv: Parser<T, A>): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val lr = lhv(input)
        return when(lr) {
            is Failure -> {
                val rr = rhv(input)
                when(rr) {
                    is Success -> Success(rr.rest, rr.result)
                    is Failure -> Failure(rr.expected, rr.location)
                }
            }
            else -> lr
        }
    }
}

infix fun <T, A> Parser<T, A>.or(that: Parser<T, A>) = OrParser(this, that).asParser()

data class RecursiveParser<T, A>(val f: (Parser<T, A>) -> Parser<T, A>): Parser<T, A> {
    val lz by kotlin.lazy{ f(this) }

    override fun invoke(input: Input<T>): ParseResult<T, A> = lz(input)
}

fun<T, A> recursive(f: (Parser<T, A>) -> Parser<T, A>) = RecursiveParser(f).asParser()

data class LazyParser<T, A>(val f: Lazy<Parser<T, A>>): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> = f.value.invoke(input)
    override fun toString(): String {
        return "LazyParser<?>"
    }
}

fun<T, A> defer(f: () -> Parser<T, A>) = LazyParser(kotlin.lazy(f)).asParser()

data class AltParser<T, A, B: A>(val element: Parser<T, B>, val default: A): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val base = element(input)
        return when(base) {
            is Success -> base
            is Failure -> Success(input, default)
        }
    }
}

infix fun <T, A> Parser<T, A>.orElse(value: A) = AltParser(this, value).asParser()
fun <T, A> Parser<T, A>.orNot() = AltParser(this, null).asParser()

data class ManyParser<T, A>(val element: Parser<T, A>): Parser<T, Collection<A>> {
    override fun invoke(input: Input<T>): ParseResult<T, Collection<A>> {
        var curInput = input
        var res = element(curInput)
        val col = mutableListOf<A>()
        while(res is Success) {
            col += res.result
            curInput = res.rest
            res = element(curInput)
        }
        return Success(curInput, col)
    }
}

fun <T, A> Parser<T, A>.many() = ManyParser(this).asParser()
fun <T, A> Parser<T, A>.manyOne() = this + ManyParser(this).asParser()

infix fun <T, A> Parser<T, A>.joinedBy(sep: Parser<T, Unit>) =
        this + (sep + this).many()
@JvmName("joinedByUniform")
infix fun <T, A> Parser<T, A>.joinedBy(sep: Parser<T, A>) =
        this + (sep + this).many().map { it.flatten() }

data class ChainParser<T, A, B>(val base: Parser<T, A>, val next: (A) -> Parser<T, B>): Parser<T, B> {
    override fun invoke(input: Input<T>): ParseResult<T, B> {
        val first = base(input)
        return when(first) {
            is Failure -> first
            is Success -> next(first.result).invoke(first.rest)
        }
    }
}

fun <T, A, B> Parser<T, A>.chain(next: (A) -> Parser<T, B>) = ChainParser(this, next).asParser()
fun <T, A> Parser<T, Parser<T, A>>.flatten() = chain { it }

class EofParser<T>: Parser<T, Unit> {
    override fun invoke(input: Input<T>) = when {
        input.source.isEmpty() -> Success(input, Unit)
        else -> Failure("<EOF>", input.location)
    }
}

fun <T> eof() = EofParser<T>().asParser()

@JvmName("plus<Any, Any>")
operator fun <T, A> Parser<T, A>.plus(that: Parser<T, A>) = zip(this, that){ a, b -> listOf(a, b) }
@JvmName("plus<Unit, Unit>")
operator fun <T> Parser<T, Unit>.plus(that: Parser<T, Unit>) = zip(this, that){ _, _ -> }
@JvmName("plus<Unit, Any>")
operator fun <T, B> Parser<T, Unit>.plus(that: Parser<T, B>) = zip(this, that) { _, b -> b }
@JvmName("plus<Any, Unit>")
operator fun <T, A> Parser<T, A>.plus(that: Parser<T, Unit>) = zip(this, that) { a, _ -> a }
@JvmName("plus<Collection, Any>")
operator fun <T, A> Parser<T, Collection<A>>.plus(that: Parser<T, A>) =
        zip(this, that) { a, b -> a + b }
@JvmName("plus<Any, Collection>")
operator fun <T, A> Parser<T, A>.plus(that: Parser<T, Collection<A>>) =
        zip(this, that) { a, b -> listOf(a) + b }
@JvmName("plus<Collection, Collection>")
operator fun <T, A> Parser<T, Collection<A>>.plus(that: Parser<T, Collection<A>>) =
        zip(this, that) { a, b -> a + b }
@JvmName("plus<Unit, Collection>")
operator fun <T, A> Parser<T, Unit>.plus(that: Parser<T, Collection<A>>) =
        zip(this, that) { _, b -> b }
@JvmName("plus<Collection, Unit>")
operator fun <T, A> Parser<T, Collection<A>>.plus(that: Parser<T, Unit>) =
        zip(this, that) { a, _ -> a }

operator fun <T, A> Parser<T, A>.unaryMinus() = map {}
