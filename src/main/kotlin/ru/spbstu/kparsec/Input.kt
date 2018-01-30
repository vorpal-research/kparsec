package ru.spbstu.kparsec

import ru.spbstu.kparsec.wheels.*

interface Location<T> {
    operator fun invoke(token: T): Location<Char>
}

data class CharLocation(val source: String, val line: Int, val col: Int): Location<Char> {
    override operator fun invoke(token: Char)= when(token) {
        '\n' -> copy(line = line + 1, col = 0)
        else -> copy(col = col + 1)
    }
}

interface Input<out T> {
    val current: T

    fun isEmpty(): Boolean
    fun next(): Input<T> = drop(1)

    fun drop(n: Int): Input<T> {
        var res = this
        repeat(n) {
            if(res.isEmpty()) return res
            res = res.next()
        }
        return res
    }

    fun asTokenSequence(): TokenSequence<T>
}

fun Input<Char>.asCharSequence(): CharSequence = asTokenSequence().asCharSequence()
fun<T> Input<T>.currentOrNull() = if(isEmpty()) null else current

data class StringInput(val string: CharSequence,
                       val offset: Int = 0
): Input<Char> {
    override fun isEmpty(): Boolean = string.length <= offset
    override val current: Char get() = string[offset]
    override fun next(): Input<Char> = copy(offset = offset + 1)
    override fun drop(n: Int): StringInput = copy(offset = offset + n)
    override fun asTokenSequence(): TokenSequence<Char> = string.asTokenSequence().subSequence(offset, string.length)
}

data class ListInput<T>(val list: List<T>,
                        val offset: Int = 0): Input<T> {
    override val current: T get() = list[offset]
    override fun isEmpty(): Boolean = list.size <= offset
    override fun next(): Input<T> = copy(offset = offset + 1)
    override fun drop(n: Int): ListInput<T> = copy(offset = offset + n)
    override fun asTokenSequence(): TokenSequence<T> = list.asTokenSequence().subSequence(offset, list.size)
}
