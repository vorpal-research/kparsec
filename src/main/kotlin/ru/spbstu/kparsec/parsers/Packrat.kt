package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.*

class PackratGrammarContext<T> : GrammarContext<T> {
    val nonTerminals: MutableMap<String, Parser<T, *>> = mutableMapOf()

    inner class PackratProxyParser<R>(val name: String) : Parser<T, R> {
        var results: MutableMap<Location<T>, ParseResult<T, R>> = mutableMapOf()

        override val description: String get() = name
        override fun invoke(input: Source<T>): ParseResult<T, R> {
            if(input.location in results) return results[input.location]!!

            results[input.location] = Error("Left recursion detected", input.location)

            val res = nonTerminals[name]?.let { it as Parser<T, R> }?.invoke(input)
                    ?: throw IllegalStateException("Uninitialized non-terminal: $name")
            results[input.location] = res

            return res
        }
    }

    override fun <R> get(name: String): Parser<T, R> = PackratProxyParser(name)

    override fun <R> set(name: String, parser: Parser<T, R>) {
        nonTerminals.put(name, parser)
    }

}

fun<T, R> packratGrammar(body: GrammarContext<T>.() -> Parser<T, R>) = PackratGrammarContext<T>().body()

