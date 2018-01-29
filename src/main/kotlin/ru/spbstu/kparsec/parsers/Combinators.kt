package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*

/**
 * [lhv] followed by [rhv], combining results using [f]
 */
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

/**
 * [lhv] followed by [rhv], combining results using [f]
 */
fun <T, A, B, R> zip(lhv: Parser<T, A>, rhv: Parser<T, B>, f: (A, B) -> R): Parser<T, R> =
        ZipParser(lhv, rhv, f).asParser()
/**
 * [lhv] followed by [mhv] and by [rhv], combining results using [f]
 */
fun <T, A, B, C, R> zip(lhv: Parser<T, A>, mhv: Parser<T, B>, rhv: Parser<T, C>, f: (A, B, C) -> R): Parser<T, R> =
        zip(lhv, zip(mhv, rhv)){ a, (b, c) -> f(a, b, c) }.asParser()
/**
 * [lhv] followed by [rhv], combining results into a [Pair]
 */
fun <T, A, B> zip(lhv: Parser<T, A>, rhv: Parser<T, B>): Parser<T, Pair<A, B>> = ZipParser(lhv, rhv, ::Pair).asParser()
/**
 * [lhv] followed by [mhv] and by [rhv], combining results into a [Triple]
 */
fun <T, A, B, C> zip(lhv: Parser<T, A>, mhv: Parser<T, B>, rhv: Parser<T, C>): Parser<T, Triple<A, B, C>> =
        zip(lhv, mhv, rhv, ::Triple)

/**
 * Apply several parsers ([elements]) one after each other, resulting in a [List]
 */
data class SeqParser<T, A>(val elements: Iterable<Parser<T, A>>): Parser<T, List<A>> {
    constructor(vararg elements: Parser<T, A>): this(elements.asList())

    override fun invoke(input: Input<T>): ParseResult<T, List<A>> {
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

/**
 * Parse several [parsers] in a row, resulting in a [List]
 */
fun <T, A> sequence(parsers: Iterable<Parser<T, A>>): Parser<T, List<A>> = SeqParser(parsers).asParser()
/**
 * Parse several [parsers] in a row, resulting in a [List]
 */
fun <T, A> sequence(vararg parsers: Parser<T, A>): Parser<T, List<A>> = SeqParser(*parsers).asParser()

/**
 * Try several parsers ([elements]) in a row, pick the first one successful
 */
data class ChoiceParser<T, A>(val elements: Iterable<Parser<T, A>>): Parser<T, A> {
    constructor(vararg elements: Parser<T, A>): this(elements.asList())

    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val it = elements.iterator()
        if(!it.hasNext()) return Failure("<empty choice>")

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

/**
 * Try several [parsers] in a row, pick the first one successful
 */
/* it is named "oneOfCollection" not to clash with oneOf for tokens */
fun <T, A> oneOfCollection(parsers: Iterable<Parser<T, A>>): Parser<T, A> = ChoiceParser(parsers).asParser()
/**
 * Try several [parsers] in a row, pick the first one successful
 */
fun <T, A> oneOf(vararg parsers: Parser<T, A>): Parser<T, A> = ChoiceParser(*parsers).asParser()

/**
 * Parser that works the same way as [lhv], but applies [f] to result.
 */
data class MapParser<T, A, R>(val lhv: Parser<T, A>, val f: (A) -> R): Parser<T, R> {
    override fun invoke(input: Input<T>): ParseResult<T, R> {
        val r = lhv(input)
        return when(r) {
            is Success -> Success(r.rest, f(r.result))
            is Failure -> r
        }
    }
}

/**
 * Parser that works the same way as [lhv], but applies [f] to result.
 */
fun <T, A, R> Parser<T, A>.map(f: (A) -> R): Parser<T, R> = MapParser(this, f).asParser()

/**
 * Parser that works the same way as [lhv], but fails if `p(result)` is `false`.
 * Does not consume any input on failure.
 */
data class FilterParser<T, A>(val lhv: Parser<T, A>, val p: (A) -> Boolean): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val r = lhv(input)
        return when {
            r is Success && p(r.result) -> r
            r is Failure -> r
            else -> Failure("filter")
        }
    }
}

/**
 * Parser that works the same way as [lhv], but fails if `p(result)` is `false`.
 * Does not consume any input on failure.
 */
fun <T, A> Parser<T, A>.filter(p: (A) -> Boolean): Parser<T, A> = FilterParser(this, p).asParser()

/**
 * Recursive parser: supplies [f] with itself lazily
 * Example:
 * ```kotlin
 *      val parens = recursive { char('(') + it + char(')') }
 * ```
 */
data class RecursiveParser<T, A>(val f: (Parser<T, A>) -> Parser<T, A>): Parser<T, A> {
    val lz by kotlin.lazy{ f(this) }

    override fun invoke(input: Input<T>): ParseResult<T, A> = lz(input)
}

/**
 * Recursive parser: supplies [f] with itself lazily
 * Example:
 * ```kotlin
 *      val parens = recursive { -char('(') + it + -char(')') }
 * ```
 * @see defer
 */
fun<T, A> recursive(f: (Parser<T, A>) -> Parser<T, A>): Parser<T, A> = RecursiveParser(f).asParser()

/**
 * Lazy parser: defers constructing [f] until the actual parsing happens
 * Example:
 * ```kotlin
 *     val inner = defer { parens }
 *     val parens = -char('(') + inner + -char(')')
 * ```
 * @see recursive
 */
data class LazyParser<T, A>(val f: Lazy<Parser<T, A>>): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> = f.value.invoke(input)
    override fun toString(): String {
        return "LazyParser<?>"
    }
}

/**
 * Lazy parser: defers constructing [f] until the actual parsing happens
 * Example:
 * ```kotlin
 *     val inner = defer { parens }
 *     val parens = -char('(') + inner + -char(')')
 * ```
 * @see recursive
 */
fun<T, A> defer(f: () -> Parser<T, A>): Parser<T, A> = LazyParser(kotlin.lazy(f)).asParser()

/**
 * [Parser] that works the same way as [element], but returns [default] if [element] fails.
 * Always succeeds.
 */
data class AltParser<T, A, B: A>(val element: Parser<T, B>, val default: A): Parser<T, A> {
    override fun invoke(input: Input<T>): ParseResult<T, A> {
        val base = element(input)
        return when(base) {
            is Success -> base
            is Failure -> Success(input, default)
        }
    }
}

/**
 * [Parser] that works the same way as [this], but returns [value] if [this] fails.
 * Always succeeds.
 */
infix fun <T, A> Parser<T, A>.orElse(value: A): Parser<T, A> = AltParser(this, value).asParser()
/**
 * [Parser] that works the same way as [this], but returns `null` if [this] fails.
 * Always succeeds.
 */
fun <T, A> Parser<T, A>.orNot(): Parser<T, A?> = AltParser(this, null).asParser()

/**
 * [Parser] that expects multiple (or zero) occurencies of [element], stopping when [element] fails.
 * Always succeeds, **even if initialized with an empty input**.
 */
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

/**
 * [Parser] that expects multiple (or zero) occurencies of [element], stopping when [element] fails.
 * Always succeeds, **even if initialized with an empty input**.
 */
fun <T, A> Parser<T, A>.many(): Parser<T, List<A>> = ManyParser(this).asParser()
/**
 * [Parser] that expects one or more occurencies of [element], stopping when [element] fails.
 * Fails if the first invocation of [element] fails.
 */
fun <T, A> Parser<T, A>.manyOne(): Parser<T, List<A>> = this + ManyParser(this).asParser()

/**
 * [Parser] that expects [element] exactly N times, where N is in range [limit].
 * Fails if the number of [element] occurencies are not in range [limit].
 * Note: if the range [limit] is empty, this parser always succeeds, returning an empty list
 */
data class LimitedManyParser<T, A>(val element: Parser<T, A>, val limit: ClosedRange<Int>): Parser<T, List<A>> {
    init {
        assert(limit.start >= 0)
        assert(limit.endInclusive >= 0)
    }
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

        if(i < limit.start) return Failure("$element * $limit")
        return Success(curInput, col)
    }
}

/**
 * [Parser] that expects [element] exactly [value] times.
 * Fails if the number of [element] occurencies is not [value].
 * Note: if [value] is 0, this parser always succeeds, returning an empty list
 */
fun <T, A> Parser<T, A>.repeated(value: Int): Parser<T, List<A>> =
        LimitedManyParser(this, value..value).asParser()
/**
 * [Parser] that expects [element] exactly N times, where N is in range [limit].
 * Fails if the number of [element] occurencies are not in range [limit].
 * Note: if the range [limit] is empty, this parser always succeeds, returning an empty list
 */
fun <T, A> Parser<T, A>.repeated(range: ClosedRange<Int>): Parser<T, List<A>> =
        LimitedManyParser(this, range).asParser()

/**
 * [Parser] that a number of [this] occurencies separated by [sep].
 * Ignores the results of [sep], returning only [this] results, wrapped in a list.
 */
infix fun <T, A> Parser<T, A>.joinedBy(sep: Parser<T, Unit>): Parser<T, List<A>> =
        this + (sep + this).many()
/**
 * [Parser] that a number of [this] occurencies separated by [sep].
 * Returns **all** the results as a list.
 */
@JvmName("joinedByUniform")
infix fun <T, A> Parser<T, A>.joinedBy(sep: Parser<T, A>): Parser<T, List<A>> =
        this + (sep + this).many().map { it.flatten() }

/**
 * Parses input using [base], feeding its results to [next] and using the results.
 */
data class ChainParser<T, A, B>(val base: Parser<T, A>, val next: (A) -> Parser<T, B>): Parser<T, B> {
    override fun invoke(input: Input<T>): ParseResult<T, B> {
        val first = base(input)
        return when(first) {
            is Failure -> first
            is Success -> next(first.result).invoke(first.rest)
        }
    }
}

/**
 * Parse input using [base], feeding its results to [next] and applying the result.
 */
infix fun <T, A, B> Parser<T, A>.chain(next: (A) -> Parser<T, B>): Parser<T, B>
        = ChainParser(this, next).asParser()

/**
 * Apply [this] and then apply the result
 */
fun <T, A> Parser<T, Parser<T, A>>.flatten(): Parser<T, A> = chain { it }
