package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Parser

@JvmName("plusAnyAny")
operator fun <T, A> Parser<T, A>.plus(that: Parser<T, A>): Parser<T, List<A>> =
        zip(this, that){ a, b -> listOf(a, b) }
@JvmName("plusUnitUnit")
operator fun <T> Parser<T, Unit>.plus(that: Parser<T, Unit>): Parser<T, Unit> = zip(this, that){ _, _ -> }
@JvmName("plusUnitAny")
operator fun <T, B> Parser<T, Unit>.plus(that: Parser<T, B>): Parser<T, B> = zip(this, that) { _, b -> b }
@JvmName("plusAnyUnit")
operator fun <T, A> Parser<T, A>.plus(that: Parser<T, Unit>): Parser<T, A> = zip(this, that) { a, _ -> a }
@JvmName("plusCollectionAny")
operator fun <T, A> Parser<T, Collection<A>>.plus(that: Parser<T, A>): Parser<T, List<A>> =
        zip(this, that) { a, b -> a + b }
@JvmName("plusAnyCollection")
operator fun <T, A> Parser<T, A>.plus(that: Parser<T, Collection<A>>): Parser<T, List<A>> =
        zip(this, that) { a, b -> listOf(a) + b }
@JvmName("plusCollectionCollection")
operator fun <T, A> Parser<T, Collection<A>>.plus(that: Parser<T, Collection<A>>): Parser<T, List<A>> =
        zip(this, that) { a, b -> a + b }
@JvmName("plusUnitCollection")
operator fun <T, A, C: Collection<A>> Parser<T, Unit>.plus(that: Parser<T, C>): Parser<T, C> =
        zip(this, that) { _, b -> b }
@JvmName("plusCollectionUnit")
operator fun <T, A, C: Collection<A>> Parser<T, C>.plus(that: Parser<T, Unit>): Parser<T, C> =
        zip(this, that) { a, _ -> a }

infix fun <T, A> Parser<T, A>.or(that: Parser<T, A>): Parser<T, A> = oneOf(this, that).asParser()

operator fun <T, A> Parser<T, A>.unaryMinus(): Parser<T, Unit> = map {}

operator fun <T, A> Parser<T, A>.times(value: Int): Parser<T, List<A>> =
        LimitedManyParser(this, value..value).asParser()
operator fun <T, A> Parser<T, A>.times(range: ClosedRange<Int>): Parser<T, List<A>> =
        LimitedManyParser(this, range).asParser()
