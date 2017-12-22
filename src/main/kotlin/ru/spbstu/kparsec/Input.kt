package ru.spbstu.kparsec

data class StringAsList(val inner: CharSequence): AbstractList<Char>(), CharSequence by inner {
    override val size: Int = inner.length

    override fun contains(element: Char) = inner.contains(element)
    override fun get(index: Int) = inner.get(index)
    override fun indexOf(element: Char) = inner.indexOf(element)
    override fun isEmpty(): Boolean = inner.isEmpty()
    override fun iterator(): Iterator<Char> = inner.iterator()
    override fun lastIndexOf(element: Char) = inner.lastIndexOf(element)

    override fun toString() = joinToString(prefix = "[", postfix = "]", separator = ", ")
}

fun CharSequence.asCharList(): List<Char> = StringAsList(this)

data class ListAsCharseq(val inner: List<Char>): List<Char> by inner, CharSequence {
    override val length = size
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            ListAsCharseq(inner.subList(startIndex, endIndex))

    override fun toString() = inner.joinToString("")
}

fun List<Char>.asCharSequence(): CharSequence = ListAsCharseq(this)

data class SubSequence(val seq: CharSequence, val from: Int, val to: Int): CharSequence {
    override val length: Int
        get() = to - from

    override fun get(index: Int): Char = seq.get(from + index)

    override fun subSequence(startIndex: Int, endIndex: Int) =
            SubSequence(seq, from + startIndex, to - endIndex)
}

data class Location(val source: String, val line: Int, val col: Int) {
    operator fun invoke(code: Char) = when(code) {
        '\n' -> copy(line = line + 1, col = 0)
        else -> copy(col = col + 1)
    }
}

interface Input<out T> {
    val source: List<T>
    val location: Location

    fun next(): Input<T>

    fun drop(n: Int): Input<T> {
        var res = this
        repeat(n) {
            if(res.source.isEmpty()) return res
            res = res.next()
        }
        return res
    }

    fun sourceAsString() = source.joinToString("")
}

fun<T> List<T>.subList(from: Int) = subList(from, size)

data class StringInput(val string: String, val offset: Int = 0): Input<Char> {
    override val source: List<Char> get() = string.asCharList().subList(offset)
    override val location: Location = Location("<string>", 0, offset)

    override fun next(): Input<Char> = copy(offset = offset + 1)
    override fun drop(n: Int) = copy(offset = offset + n)

    override fun sourceAsString() = string.drop(offset)
}
