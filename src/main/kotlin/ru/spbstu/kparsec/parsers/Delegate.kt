package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.ParseResult
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.Source

/**
 * A parser delegating its implementation to another parser.
 * This is just a convenience interface, because parsers are often defined as `object`s
 * if the grammar is recursive, for example:
 * ```kotlin
 *    object TheParser: DelegateParser<Char, Unit> {
 *        val parens = -char('(') + defer{ self } + -char(')')
 *        override val self = parens
 *    }
 * ```
 */
interface DelegateParser<T, R>: Parser<T, R> {
    val self: Parser<T, R>

    override fun invoke(input: Source<T>): ParseResult<T, R> = self.invoke(input)
    override val description: String
        get() = self.description
}
