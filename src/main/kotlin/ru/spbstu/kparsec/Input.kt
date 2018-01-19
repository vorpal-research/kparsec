package ru.spbstu.kparsec

import ru.spbstu.kparsec.wheels.asCharList

data class Location(val source: String, val line: Int, val col: Int) {
    operator fun invoke(code: Char) = when(code) {
        '\n' -> copy(line = line + 1, col = 0)
        else -> copy(col = col + 1)
    }
}

interface Input<out T> {
    val source: List<T>
    val location: Location

    fun next(): Input<T> = drop(1)

    fun drop(n: Int): Input<T> {
        var res = this
        repeat(n) {
            if(res.source.isEmpty()) return res
            res = res.next()
        }
        return res
    }

    fun sourceAsString(): String = source.joinToString("")
}

fun<T> List<T>.subList(from: Int): List<T> = subList(from, size)

data class StringInput(val string: String, val offset: Int = 0): Input<Char> {
    override val source: List<Char> get() = string.asCharList().subList(offset)
    override val location: Location = Location("<string>", 0, offset)

    override fun next(): Input<Char> = copy(offset = offset + 1)
    override fun drop(n: Int): StringInput = copy(offset = offset + n)

    override fun sourceAsString(): String = string.drop(offset)
}

data class ListInput<T>(val data: List<T>, val offset: Int = 0): Input<T> {
    override val source: List<T> get() = data.subList(offset)
    override val location: Location = Location("<string>", 0, offset)

    override fun next(): Input<T> = copy(offset = offset + 1)
    override fun drop(n: Int): ListInput<T> = copy(offset = offset + n)

    override fun sourceAsString(): String = source.joinToString("")
}
