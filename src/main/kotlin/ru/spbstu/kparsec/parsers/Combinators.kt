package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*

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
fun <T, A, B, C, R> zip(lhv: Parser<T, A>, mhv: Parser<T, B>, rhv: Parser<T, C>, f: (A, B, C) -> R) =
        zip(lhv, zip(mhv, rhv)){ a, (b, c) -> f(a, b, c) }.asParser()
fun <T, A, B> zip(lhv: Parser<T, A>, rhv: Parser<T, B>) = ZipParser(lhv, rhv, ::Pair).asParser()
fun <T, A, B, C> zip(lhv: Parser<T, A>, mhv: Parser<T, B>, rhv: Parser<T, C>) = zip(lhv, mhv, rhv, ::Triple)

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
            is Success -> Success(currentResult.rest, result.apply { add(currentResult.result) })
            is Failure -> currentResult
        }
    }
}

fun <T, A> sequence(parsers: Iterable<Parser<T, A>>) = SeqParser(parsers).asParser()
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

/* it is named "oneOfCollection" not to clash with oneOf for tokens */
fun <T, A> oneOfCollection(parsers: Iterable<Parser<T, A>>) = ChoiceParser(parsers).asParser()
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

data class FilterParser<T, A>(val lhv: Parser<T, A>, val p: (A) -> Boolean): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val r = lhv(input)
        return when {
            r is Success && p(r.result) -> r
            r is Failure -> r
            else -> Failure("filter", input.location)
        }
    }
}

fun <T, A> Parser<T, A>.filter(p: (A) -> Boolean) = FilterParser(this, p).asParser()

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

data class ManyParser<T, A>(val element: Parser<T, A>): Parser<T, List<A>> {
    override fun invoke(input: Input<T>): ParseResult<T, List<A>> {
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

data class LimitedManyParser<T, A>(val element: Parser<T, A>, val limit: ClosedRange<Int>): Parser<T, List<A>> {
    override fun invoke(input: Input<T>): ParseResult<T, List<A>> {
        var curInput = input
        var res = element(curInput)
        val col = mutableListOf<A>()
        var i = 0
        while(res is Success && i < limit.endInclusive) {
            ++i
            col += res.result
            curInput = res.rest
            res = element(curInput)
        }

        if(i < limit.start) return Failure("$element * $limit", curInput.location)
        return Success(curInput, col)
    }
}

fun <T, A> Parser<T, A>.repeated(value: Int) =
        LimitedManyParser(this, value..value).asParser()
fun <T, A> Parser<T, A>.repeated(range: ClosedRange<Int>) =
        LimitedManyParser(this, range).asParser()

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

infix fun <T, A, B> Parser<T, A>.chain(next: (A) -> Parser<T, B>) = ChainParser(this, next).asParser()
fun <T, A> Parser<T, Parser<T, A>>.flatten() = chain { it }
