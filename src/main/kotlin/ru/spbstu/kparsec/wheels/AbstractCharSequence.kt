package ru.spbstu.kparsec.wheels

data class CharSubSequence(val seq: CharSequence, val from: Int, val to: Int): AbstractCharSequence() {
    override val length: Int
        get() = to - from

    override fun get(index: Int): Char = when {
        index !in 0..length -> throw IndexOutOfBoundsException()
        else -> seq[from + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int) = when {
        endIndex !in 0..length -> throw IndexOutOfBoundsException()
        else -> CharSubSequence(seq, from + startIndex, from + endIndex)
    }

    /* this is NOT redundant, as data classes override base classes' toString by default */
    override fun toString() = super.toString()
}

private fun CharSequence.ethemeralSubSequence(startIndex: Int, endIndex: Int) = when(this) {
    is CharSubSequence -> subSequence(startIndex, endIndex)
    else -> CharSubSequence(this, startIndex, endIndex)
}

abstract class AbstractCharSequence: CharSequence {
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            CharSubSequence(this, startIndex, endIndex)

    override fun toString(): String {
        val sb = StringBuilder()
        for(i in 0 until length) {
            sb.append(get(i))
        }
        return sb.toString()
    }
}

data class ListAsCharSequence(val inner: List<Char>): AbstractCharSequence() {
    override val length: Int = inner.size

    override fun get(index: Int): Char = inner.get(index)
}

fun List<Char>.asCharSequence(): CharSequence = when(this) {
    is CharSequenceAsList -> inner
    else -> ListAsCharSequence(this)
}

data class CharSequenceAsList(val inner: CharSequence): AbstractList<Char>() {
    override val size: Int get() = inner.length
    override fun get(index: Int): Char = inner.get(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<Char> =
            CharSequenceAsList(inner.ethemeralSubSequence(fromIndex, toIndex))
}

fun CharSequence.asCharList(): List<Char> = when(this) {
    is ListAsCharSequence -> inner
    else -> CharSequenceAsList(this)
}
