package ru.spbstu.kparsec

/**
 * The basic parser interface.
 *
 * @param T the type of input tokens (Char for simple cases)
 * @param R the type of result of parsing
 */
interface Parser<T, out R> {
    /**
     * Perform parsing on [input]
     * @see ParseResult
     * @return the result of parsing
     */
    operator fun invoke(input: Input<T>): ParseResult<T, R>
}

/**
 * Parse a string
 * @see StringInput
 */
fun<T> Parser<Char, T>.parse(string: String): ParseResult<Char, T> = this(StringInput(string))

/**
 * Parse a list of tokens
 * @see ListInput
 */
fun<T, E> Parser<T, E>.parse(data: List<T>): ParseResult<T, E> = this(ListInput(data))

/**
 * Parse an array of tokens
 * @see ListInput
 */
fun<T, E> Parser<T, E>.parse(data: Array<T>): ParseResult<T, E> = this(ListInput(data.asList()))
