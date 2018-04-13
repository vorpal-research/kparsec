package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.ParseResult
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.Source
import kotlin.reflect.KProperty

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

object NonTerminal
interface GrammarContext<T> {
    fun<R> get(name: String): Parser<T, R>
    fun<R> set(name: String, parser: Parser<T, R>)

    operator fun<R> NonTerminal.getValue(self: Any?, prop: KProperty<*>): Parser<T, R> = get(prop.name)
    operator fun<R> NonTerminal.setValue(self: Any?, prop: KProperty<*>, value: Parser<T, R>) = set(prop.name, value)

}

class SimpleGrammarContext<T> : GrammarContext<T> {
    val nonTerminals: MutableMap<String, Parser<T, *>> = mutableMapOf()

    inner class ProxyParser<R>(val name: String) : Parser<T, R> {
        override val description: String get() = name
        override fun invoke(input: Source<T>): ParseResult<T, R> =
                nonTerminals[name]?.let { it as Parser<T, R> }?.invoke(input)
                        ?: throw IllegalStateException("Uninitialized non-terminal: $name")
    }

    override fun<R> get(name: String): Parser<T, R> = ProxyParser(name)

    override fun <R> set(name: String, parser: Parser<T, R>) {
        nonTerminals.put(name, parser)
    }
}

fun<T, R> grammar(body: GrammarContext<T>.() -> Parser<T, R>) = SimpleGrammarContext<T>().body()
